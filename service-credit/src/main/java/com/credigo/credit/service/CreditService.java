package com.credigo.credit.service;

import com.credigo.credit.dto.*;
import com.credigo.credit.entite.LoanApplication;
import com.credigo.credit.entite.RepaymentSchedule;
import com.credigo.credit.enums.StatutDossier;
import com.credigo.credit.enums.StatutEcheance;
import com.credigo.credit.exception.RegleMetierException;
import com.credigo.credit.exception.RessourceIntrouvableException;
import com.credigo.credit.feign.ClientDistantDto;
import com.credigo.credit.feign.DecaissementDistantDto;
import com.credigo.credit.feign.GarantieDistanteDto;
import com.credigo.credit.feign.ResilientRequests;
import com.credigo.credit.mapper.CreditMapper;
import com.credigo.credit.messaging.NotificationEmailDto;
import com.credigo.credit.messaging.NotificationPublisher;
import com.credigo.credit.repository.LoanApplicationRepository;
import com.credigo.credit.repository.RepaymentScheduleRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Cycle de vie du dossier de crédit :
 * DEPOSE --etude--> EN_ETUDE --decision--> ACCORDE --deblocage--> DEBLOQUE, ou EN_ETUDE -> REFUSE.
 * Les références distantes (client, garanties) sont validées via OpenFeign (V4) avant toute écriture.
 */
@Service
@Transactional
public class CreditService {

    private final LoanApplicationRepository dossierRepository;
    private final RepaymentScheduleRepository echeanceRepository;
    private final CreditMapper mapper;
    private final ResilientRequests distant;
    private final NotificationPublisher notificationPublisher;

    public CreditService(LoanApplicationRepository dossierRepository,
                         RepaymentScheduleRepository echeanceRepository, CreditMapper mapper,
                         ResilientRequests distant, NotificationPublisher notificationPublisher) {
        this.dossierRepository = dossierRepository;
        this.echeanceRepository = echeanceRepository;
        this.mapper = mapper;
        this.distant = distant;
        this.notificationPublisher = notificationPublisher;
    }

    // ============================ Consultation ============================

    @Transactional(readOnly = true)
    public List<LoanApplicationDto> lister(Long clientId, StatutDossier statut) {
        List<LoanApplication> dossiers;
        if (clientId != null) {
            dossiers = dossierRepository.findByClientId(clientId);
        } else if (statut != null) {
            dossiers = dossierRepository.findByStatut(statut);
        } else {
            dossiers = dossierRepository.findAll();
        }
        return dossiers.stream().map(mapper::toDto).toList();
    }

    @Transactional(readOnly = true)
    public LoanApplicationDto consulter(Long id) {
        return mapper.toDto(trouver(id));
    }

    @Transactional(readOnly = true)
    public List<RepaymentScheduleDto> echeancier(Long id) {
        trouver(id);
        return echeanceRepository.findByLoanApplicationIdOrderByNumeroEcheance(id).stream()
                .map(mapper::toDto).toList();
    }

    /** Consultation enrichie : dossier + client + garanties + échéancier + décaissements. */
    @Transactional(readOnly = true)
    public DossierDetailDto consulterDetail(Long id) {
        LoanApplication dossier = trouver(id);
        ClientDistantDto client = distant.clientTolerant(dossier.getClientId());
        List<GarantieDistanteDto> garanties = distant.garantiesTolerant(dossier.getClientId());
        List<DecaissementDistantDto> decaissements = distant.decaissementsTolerant(id);
        List<RepaymentScheduleDto> echeancier = dossier.getEcheancier().stream().map(mapper::toDto).toList();
        return new DossierDetailDto(mapper.toDto(dossier), client, garanties, echeancier, decaissements,
                client == null);
    }

    // ============================ Dépôt (création inter-services) ============================

    /**
     * Dépôt d'une demande. Idempotent (V6) : une requête rejouée avec la même clé
     * Idempotency-Key renvoie le dossier déjà créé, sans en créer un second.
     */
    public ResultatDepot deposer(DemandeCreditDto dto, String idempotencyKey) {
        String cle = normaliserCle(idempotencyKey);
        if (cle != null) {
            Optional<LoanApplication> existant = dossierRepository.findByIdempotencyKey(cle);
            if (existant.isPresent()) {
                return new ResultatDepot(mapper.toDto(existant.get()), false);
            }
        }
        verifierClientActif(dto.clientId()); // référence distante validée AVANT l'enregistrement

        LoanApplication dossier = new LoanApplication();
        mapper.copierDemande(dto, dossier);
        dossier.setIdempotencyKey(cle);
        dossier.setStatut(StatutDossier.DEPOSE);
        return new ResultatDepot(mapper.toDto(dossierRepository.save(dossier)), true);
    }

    public LoanApplicationDto modifier(Long id, DemandeCreditDto dto) {
        LoanApplication dossier = trouver(id);
        exigerStatut(dossier, StatutDossier.DEPOSE, "modifié");
        if (!dossier.getClientId().equals(dto.clientId())) {
            verifierClientActif(dto.clientId());
        }
        mapper.copierDemande(dto, dossier);
        return mapper.toDto(dossier);
    }

    public void supprimer(Long id) {
        LoanApplication dossier = trouver(id);
        if (dossier.getStatut() != StatutDossier.DEPOSE && dossier.getStatut() != StatutDossier.REFUSE) {
            throw new RegleMetierException("Suppression impossible : le dossier " + id + " est "
                    + dossier.getStatut());
        }
        dossierRepository.delete(dossier);
    }

    // ============================ Étude et décision ============================

    /** DEPOSE -> EN_ETUDE : le score du client est relevé auprès du service-clients. */
    public LoanApplicationDto etudier(Long id) {
        LoanApplication dossier = trouver(id);
        exigerStatut(dossier, StatutDossier.DEPOSE, "mis à l'étude");
        ClientDistantDto client = verifierClientActif(dossier.getClientId());
        dossier.setScoreEtude(client.scoreInterne() == null ? 0 : client.scoreInterne());
        dossier.setStatut(StatutDossier.EN_ETUDE);
        return mapper.toDto(dossier);
    }

    /**
     * EN_ETUDE -> ACCORDE ou REFUSE selon {@link RegleOctroi}. En cas d'accord, l'échéancier est
     * généré INACTIF (il ne sera activé qu'après le versement effectif) et le client est notifié.
     */
    public LoanApplicationDto decider(Long id) {
        LoanApplication dossier = trouver(id);
        exigerStatut(dossier, StatutDossier.EN_ETUDE, "décidé");
        ClientDistantDto client = distant.clientStrict(dossier.getClientId());

        int score = dossier.getScoreEtude() == null ? 0 : dossier.getScoreEtude();
        BigDecimal taux = RegleOctroi.tauxPourScore(score);
        if (!client.estActif()) {
            return refuser(dossier, "Client inactif");
        }
        if (taux == null) {
            return refuser(dossier, "Score insuffisant (" + score + " < " + RegleOctroi.SCORE_MINIMUM + ")");
        }

        BigDecimal capacite = client.revenusMensuels().multiply(RegleOctroi.TAUX_ENDETTEMENT_MAX);
        BigDecimal montantAccorde = dossier.getMontantDemande();
        String commentaire = "Accord total";
        if (RegleOctroi.premiereMensualite(montantAccorde, dossier.getDureeMois(), taux).compareTo(capacite) > 0) {
            montantAccorde = RegleOctroi.montantMaximal(capacite, dossier.getDureeMois(), taux);
            BigDecimal moitie = dossier.getMontantDemande().divide(new BigDecimal("2"), 2, RoundingMode.HALF_UP);
            if (montantAccorde.compareTo(moitie) < 0) {
                return refuser(dossier, "Taux d'endettement supérieur à 40 % des revenus");
            }
            commentaire = "Accord partiel limité par le taux d'endettement (40 % des revenus)";
        }
        if (montantAccorde.compareTo(RegleOctroi.SEUIL_GARANTIES) > 0) {
            BigDecimal totalGaranties = distant.garantiesStrict(dossier.getClientId()).stream()
                    .map(GarantieDistanteDto::valeur).reduce(BigDecimal.ZERO, BigDecimal::add);
            if (totalGaranties.compareTo(montantAccorde.multiply(RegleOctroi.COUVERTURE_GARANTIES)) < 0) {
                return refuser(dossier, "Garanties insuffisantes (moins de 30 % du montant)");
            }
        }

        dossier.setMontantAccorde(montantAccorde);
        dossier.setTauxPropose(taux);
        dossier.setCommentaireDecision(commentaire);
        dossier.setStatut(StatutDossier.ACCORDE);
        genererEcheancier(dossier);

        notificationPublisher.publierApresCommit(new NotificationEmailDto("LOAN_APPROVED", dossier.getId(),
                client.email(), "Credi-Go : votre crédit n° " + dossier.getId() + " est accordé",
                "Bonjour " + client.nom() + ",\n\nVotre demande de crédit n° " + dossier.getId()
                        + " est accordée : " + montantAccorde + " MAD sur " + dossier.getDureeMois()
                        + " mois au taux de " + taux + " %.\nLes fonds vous seront versés prochainement.\n\nCredi-Go"));
        return mapper.toDto(dossier);
    }

    // ============================ Déblocage (appelé par service-decaissements) ============================

    /**
     * ACCORDE -> DEBLOQUE et activation de l'échéancier, uniquement après le versement effectif.
     * Idempotent : un second appel sur un dossier déjà DEBLOQUE ne modifie rien.
     */
    public LoanApplicationDto debloquer(Long id) {
        LoanApplication dossier = trouver(id);
        if (dossier.getStatut() == StatutDossier.DEBLOQUE) {
            return mapper.toDto(dossier);
        }
        exigerStatut(dossier, StatutDossier.ACCORDE, "débloqué");
        dossier.setStatut(StatutDossier.DEBLOQUE);
        dossier.getEcheancier().forEach(e -> e.setStatut(StatutEcheance.A_PAYER));
        return mapper.toDto(dossier);
    }

    // ============================ Outils ============================

    private LoanApplicationDto refuser(LoanApplication dossier, String raison) {
        dossier.setStatut(StatutDossier.REFUSE);
        dossier.setCommentaireDecision(raison);
        return mapper.toDto(dossier);
    }

    /** Amortissement constant : même part de capital chaque mois, intérêts sur le capital restant dû. */
    private void genererEcheancier(LoanApplication dossier) {
        dossier.getEcheancier().clear();
        int n = dossier.getDureeMois();
        BigDecimal montant = dossier.getMontantAccorde();
        BigDecimal tauxMensuel = RegleOctroi.tauxMensuel(dossier.getTauxPropose());
        BigDecimal capital = montant.divide(BigDecimal.valueOf(n), 2, RoundingMode.HALF_UP);
        BigDecimal restant = montant;
        LocalDate premiereDate = LocalDate.now().plusMonths(1).withDayOfMonth(5);
        for (int i = 1; i <= n; i++) {
            BigDecimal part = (i == n) ? restant : capital; // la dernière échéance absorbe les arrondis
            RepaymentSchedule e = new RepaymentSchedule();
            e.setLoanApplication(dossier);
            e.setNumeroEcheance(i);
            e.setDateEcheance(premiereDate.plusMonths(i - 1L));
            e.setMontantCapital(part);
            e.setMontantInterets(restant.multiply(tauxMensuel).setScale(2, RoundingMode.HALF_UP));
            e.setStatut(StatutEcheance.INACTIVE);
            dossier.getEcheancier().add(e);
            restant = restant.subtract(part);
        }
    }

    private ClientDistantDto verifierClientActif(Long clientId) {
        ClientDistantDto client = distant.clientStrict(clientId); // 404 si inexistant, 503 si indisponible
        if (!client.estActif()) {
            throw new RegleMetierException("Le client " + clientId + " est inactif : opération refusée");
        }
        return client;
    }

    private void exigerStatut(LoanApplication dossier, StatutDossier attendu, String action) {
        if (dossier.getStatut() != attendu) {
            throw new RegleMetierException("Le dossier " + dossier.getId() + " est " + dossier.getStatut()
                    + " : seul un dossier " + attendu + " peut être " + action);
        }
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

    LoanApplication trouver(Long id) {
        return dossierRepository.findById(id)
                .orElseThrow(() -> new RessourceIntrouvableException("Dossier de crédit " + id + " introuvable"));
    }
}
