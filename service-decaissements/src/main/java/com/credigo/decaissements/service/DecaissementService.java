package com.credigo.decaissements.service;

import com.credigo.decaissements.dto.DemandeDecaissementDto;
import com.credigo.decaissements.dto.DisbursementDto;
import com.credigo.decaissements.dto.ResultatDecaissement;
import com.credigo.decaissements.entite.Disbursement;
import com.credigo.decaissements.entite.TreasuryAccount;
import com.credigo.decaissements.enums.StatutDecaissement;
import com.credigo.decaissements.exception.RegleMetierException;
import com.credigo.decaissements.exception.RessourceIntrouvableException;
import com.credigo.decaissements.feign.ClientDistantDto;
import com.credigo.decaissements.feign.DossierDistantDto;
import com.credigo.decaissements.feign.ResilientRequests;
import com.credigo.decaissements.mapper.DecaissementMapper;
import com.credigo.decaissements.messaging.NotificationEmailDto;
import com.credigo.decaissements.messaging.NotificationPublisher;
import com.credigo.decaissements.repository.DisbursementRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class DecaissementService {

    private final DisbursementRepository disbursementRepository;
    private final TresorerieService tresorerieService;
    private final DecaissementMapper mapper;
    private final ResilientRequests distant;
    private final NotificationPublisher notificationPublisher;

    public DecaissementService(DisbursementRepository disbursementRepository, TresorerieService tresorerieService,
                               DecaissementMapper mapper, ResilientRequests distant,
                               NotificationPublisher notificationPublisher) {
        this.disbursementRepository = disbursementRepository;
        this.tresorerieService = tresorerieService;
        this.mapper = mapper;
        this.distant = distant;
        this.notificationPublisher = notificationPublisher;
    }

    @Transactional(readOnly = true)
    public List<DisbursementDto> lister() {
        return disbursementRepository.findAll().stream().map(mapper::toDto).toList();
    }

    @Transactional(readOnly = true)
    public DisbursementDto consulter(Long id) {
        return disbursementRepository.findById(id).map(mapper::toDto)
                .orElseThrow(() -> new RessourceIntrouvableException("Décaissement " + id + " introuvable"));
    }

    @Transactional(readOnly = true)
    public List<DisbursementDto> listerParDossier(Long loanApplicationId) {
        return disbursementRepository.findByLoanApplicationId(loanApplicationId).stream()
                .map(mapper::toDto).toList();
    }

    /**
     * Versement du montant accordé d'un dossier.
     * <ol>
     *   <li>Idempotence (V6) : même Idempotency-Key -> on renvoie le versement déjà effectué ;</li>
     *   <li>un dossier n'est versé qu'une seule fois ;</li>
     *   <li>OpenFeign : le dossier doit être ACCORDE (service-credit) et le client ACTIF (service-clients) ;</li>
     *   <li>réservation puis débit de la trésorerie (solde suffisant) dans la transaction locale ;</li>
     *   <li>OpenFeign : service-credit passe le dossier à DEBLOQUE et active l'échéancier.</li>
     * </ol>
     * Si l'étape 5 échoue, l'exception annule la transaction locale : le débit et le versement
     * ne sont pas enregistrés, les fonds réservés sont donc libérés (compensation).
     */
    public ResultatDecaissement verser(DemandeDecaissementDto demande, String idempotencyKey) {
        String cle = normaliserCle(idempotencyKey);
        if (cle != null) {
            Optional<Disbursement> existant = disbursementRepository.findByIdempotencyKey(cle);
            if (existant.isPresent()) {
                return new ResultatDecaissement(mapper.toDto(existant.get()), false);
            }
        }
        Long dossierId = demande.loanApplicationId();
        if (disbursementRepository.existsByLoanApplicationIdAndStatut(dossierId, StatutDecaissement.VERSE)) {
            throw new RegleMetierException("Le dossier " + dossierId + " a déjà été versé : un seul versement autorisé");
        }

        DossierDistantDto dossier = distant.dossier(dossierId);
        if (!"ACCORDE".equals(dossier.statut())) {
            throw new RegleMetierException("Le dossier " + dossierId + " est " + dossier.statut()
                    + " : seul un dossier ACCORDE peut être versé");
        }
        ClientDistantDto client = distant.client(dossier.clientId());
        if (!client.estActif()) {
            throw new RegleMetierException("Le client " + client.id() + " est inactif : versement refusé");
        }

        // Réservation : contrôle et débit atomiques du solde dans la transaction locale
        TreasuryAccount compte = tresorerieService.trouver(demande.treasuryAccountId());
        BigDecimal montant = dossier.montantAccorde();
        if (compte.getSoldeDisponible().compareTo(montant) < 0) {
            throw new RegleMetierException("Solde insuffisant sur le compte " + compte.getId() + " ("
                    + compte.getSoldeDisponible() + " MAD disponibles pour " + montant + " MAD)");
        }
        compte.setSoldeDisponible(compte.getSoldeDisponible().subtract(montant));

        Disbursement versement = new Disbursement();
        versement.setIdempotencyKey(cle);
        versement.setLoanApplicationId(dossierId);
        versement.setTreasuryAccount(compte);
        versement.setMontant(montant);
        versement.setDate(LocalDateTime.now());
        versement.setStatut(StatutDecaissement.VERSE);
        disbursementRepository.save(versement);

        // Activation de l'échéancier côté service-credit : en cas d'échec -> rollback (fonds libérés)
        distant.debloquer(dossierId);

        notificationPublisher.publierApresCommit(new NotificationEmailDto("DISBURSEMENT_PAID", dossierId,
                client.email(), "Credi-Go : fonds versés pour votre crédit n° " + dossierId,
                "Bonjour " + client.nom() + ",\n\nLe montant de " + montant + " MAD de votre crédit n° "
                        + dossierId + " a été versé. Votre échéancier de remboursement est désormais actif.\n\nCredi-Go"));
        return new ResultatDecaissement(mapper.toDto(versement), true);
    }

    private String normaliserCle(String cle) {
        if (cle == null || cle.isBlank()) {
            return null;
        }
        if (cle.length() > 100) {
            throw new RegleMetierException("Clé d'idempotence trop longue (100 caractères maximum)");
        }
        return cle.trim();
    }
}
