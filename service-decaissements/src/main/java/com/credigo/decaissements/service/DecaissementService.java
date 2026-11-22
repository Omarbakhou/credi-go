package com.credigo.decaissements.service;

import com.credigo.decaissements.dto.DisbursementDto;
import com.credigo.decaissements.entite.Disbursement;
import com.credigo.decaissements.entite.TreasuryAccount;
import com.credigo.decaissements.enums.StatutDecaissement;
import com.credigo.decaissements.exception.RegleMetierException;
import com.credigo.decaissements.exception.RessourceIntrouvableException;
import com.credigo.decaissements.mapper.DecaissementMapper;
import com.credigo.decaissements.messaging.ConsommateurIdempotent;
import com.credigo.decaissements.messaging.EvenementMetier;
import com.credigo.decaissements.messaging.Topics;
import com.credigo.decaissements.messaging.donnees.CreditAccordeDonnees;
import com.credigo.decaissements.messaging.donnees.VersementEchoueDonnees;
import com.credigo.decaissements.messaging.donnees.VersementEffectueDonnees;
import com.credigo.decaissements.messaging.outbox.OutboxService;
import com.credigo.decaissements.projection.ClientProjection;
import com.credigo.decaissements.projection.ProjectionClientService;
import com.credigo.decaissements.repository.DisbursementRepository;
import com.credigo.decaissements.repository.TreasuryAccountRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;

/**
 * Décaissements (V7), piloté par événements :
 * <ol>
 *   <li>loan-approved : préparation du décaissement du montantAccorde et réservation ATOMIQUE des fonds
 *   (RESERVE) ; l'échéancier reste inactif côté service-credit ;</li>
 *   <li>POST /decaissements/{id}/versement : versement effectif, débit des fonds réservés, disbursement-paid ;</li>
 *   <li>échec (client inactif, trésorerie insuffisante), annulation ou expiration : fonds réservés libérés
 *   (compensation), statut ECHOUE / ANNULE, disbursement-failed.</li>
 * </ol>
 * Un décaissement ANNULE ou ECHOUE ne peut plus être versé : une action tardive ne le réactive pas.
 * service-decaissements décide seul sur ses données (choix du compte, solde), les données client
 * viennent de la projection locale alimentée par client-events.
 */
@Service
@Transactional
public class DecaissementService {

    private static final EnumSet<StatutDecaissement> EN_COURS_OU_VERSE =
            EnumSet.of(StatutDecaissement.RESERVE, StatutDecaissement.VERSE);

    private final DisbursementRepository disbursementRepository;
    private final TreasuryAccountRepository compteRepository;
    private final DecaissementMapper mapper;
    private final ProjectionClientService projectionClients;
    private final OutboxService outbox;
    private final ConsommateurIdempotent lecteur;
    private final Duration delaiVersement;

    public DecaissementService(DisbursementRepository disbursementRepository, TreasuryAccountRepository compteRepository,
                               DecaissementMapper mapper, ProjectionClientService projectionClients,
                               OutboxService outbox, ConsommateurIdempotent lecteur,
                               @Value("${credigo.decaissements.delai-versement:PT10M}") Duration delaiVersement) {
        this.disbursementRepository = disbursementRepository;
        this.compteRepository = compteRepository;
        this.mapper = mapper;
        this.projectionClients = projectionClients;
        this.outbox = outbox;
        this.lecteur = lecteur;
        this.delaiVersement = delaiVersement;
    }

    // ============================ Consultation ============================

    @Transactional(readOnly = true)
    public List<DisbursementDto> lister() {
        return disbursementRepository.findAll().stream().map(mapper::toDto).toList();
    }

    @Transactional(readOnly = true)
    public DisbursementDto consulter(Long id) {
        return mapper.toDto(trouver(id));
    }

    @Transactional(readOnly = true)
    public List<DisbursementDto> listerParDossier(Long loanApplicationId) {
        return disbursementRepository.findByLoanApplicationIdOrderById(loanApplicationId).stream()
                .map(mapper::toDto).toList();
    }

    // ============================ loan-approved : préparation ============================

    public String surCreditAccorde(EvenementMetier e) {
        CreditAccordeDonnees accord = lecteur.donnees(e, CreditAccordeDonnees.class);
        Long dossierId = accord.dossierId();
        if (disbursementRepository.existsByLoanApplicationIdAndStatutIn(dossierId, EN_COURS_OU_VERSE)) {
            return "IGNORE : le dossier " + dossierId + " a déjà un décaissement réservé ou versé";
        }
        BigDecimal montant = accord.montantAccorde();

        // Réservation atomique sur le premier compte dont le solde suffit (UPDATE conditionnel)
        TreasuryAccount compteReserve = null;
        Optional<ClientProjection> client = projectionClients.client(accord.clientId());
        String motifRefus = null;
        if (client.isEmpty()) {
            motifRefus = "Client " + accord.clientId() + " inconnu de service-decaissements";
        } else if (!client.get().estActif()) {
            motifRefus = "Client " + accord.clientId() + " inactif : versement refusé";
        } else {
            for (TreasuryAccount compte : compteRepository
                    .findBySoldeDisponibleGreaterThanEqualOrderBySoldeDisponibleDescIdAsc(montant)) {
                if (compteRepository.reserver(compte.getId(), montant) == 1) {
                    compteReserve = compteRepository.getReferenceById(compte.getId());
                    break;
                }
            }
            if (compteReserve == null) {
                motifRefus = "Trésorerie insuffisante : aucun compte ne couvre " + montant + " MAD";
            }
        }

        Disbursement d = new Disbursement();
        d.setLoanApplicationId(dossierId);
        d.setClientId(accord.clientId());
        d.setMontant(montant);
        d.setCorrelationId(e.correlationId());
        d.setTentative(accord.tentative());
        d.setDateReservation(LocalDateTime.now());
        if (motifRefus != null) {
            d.setStatut(StatutDecaissement.ECHOUE);
            d.setMotifEchec(motifRefus);
            disbursementRepository.saveAndFlush(d);
            publierEchec(d, BigDecimal.ZERO, e.eventId());
            return "ECHOUE : " + motifRefus;
        }
        d.setTreasuryAccount(compteReserve);
        d.setDateExpiration(LocalDateTime.now().plus(delaiVersement));
        d.setStatut(StatutDecaissement.RESERVE);
        disbursementRepository.save(d);
        return "RESERVE : " + montant + " MAD sur le compte " + compteReserve.getId();
    }

    // ============================ Versement effectif ============================

    /**
     * Versement d'un décaissement RESERVE. Idempotent : un décaissement déjà VERSE est renvoyé tel quel.
     * Si le client est devenu inactif ou si la réservation a expiré, la réservation est libérée et
     * disbursement-failed publié (commit), puis 409 : noRollbackFor conserve cette compensation.
     */
    @Transactional(noRollbackFor = RegleMetierException.class)
    public DisbursementDto verser(Long id) {
        Disbursement d = trouver(id);
        switch (d.getStatut()) {
            case VERSE:
                return mapper.toDto(d);
            case ANNULE:
            case ECHOUE:
                throw new RegleMetierException("Le décaissement " + id + " est " + d.getStatut() + " ("
                        + d.getMotifEchec() + ") : versement impossible, relancer depuis service-credit");
            default:
                break;
        }
        if (d.getDateExpiration() != null && d.getDateExpiration().isBefore(LocalDateTime.now())) {
            liberer(d, StatutDecaissement.ANNULE, "Délai de versement dépassé : réservation expirée", null);
            throw new RegleMetierException("Réservation " + id + " expirée : fonds libérés, versement refusé");
        }
        Optional<ClientProjection> client = projectionClients.client(d.getClientId());
        if (client.isEmpty() || !client.get().estActif()) {
            liberer(d, StatutDecaissement.ECHOUE, "Client " + d.getClientId() + " inactif ou inconnu au versement", null);
            throw new RegleMetierException("Client " + d.getClientId() + " inactif : versement refusé, fonds libérés");
        }

        Long compteId = d.getTreasuryAccount().getId();
        if (compteRepository.debiterReserve(compteId, d.getMontant()) != 1) {
            throw new IllegalStateException("Fonds réservés introuvables sur le compte " + compteId);
        }
        d = trouver(id); // contexte de persistance vidé par la requête de mise à jour
        d.setStatut(StatutDecaissement.VERSE);
        d.setDate(LocalDateTime.now());
        disbursementRepository.saveAndFlush(d);
        ClientProjection c = client.get();
        outbox.enregistrer(Topics.DISBURSEMENT_PAID, Topics.TYPE_DISBURSEMENT_PAID, String.valueOf(d.getLoanApplicationId()),
                d.getVersion(), d.getCorrelationId(), null,
                new VersementEffectueDonnees(d.getId(), d.getLoanApplicationId(), d.getClientId(), c.getNom(),
                        c.getEmail(), compteId, d.getMontant(), d.getDate()));
        return mapper.toDto(d);
    }

    /** Annulation par l'opérateur d'une réservation non versée : fonds libérés, disbursement-failed. */
    public DisbursementDto annuler(Long id) {
        Disbursement d = trouver(id);
        if (d.getStatut() == StatutDecaissement.VERSE) {
            throw new RegleMetierException("Le décaissement " + id + " est déjà VERSE : annulation impossible");
        }
        if (d.getStatut() == StatutDecaissement.RESERVE) {
            d = liberer(d, StatutDecaissement.ANNULE, "Réservation annulée par l'opérateur", null);
        }
        return mapper.toDto(d);
    }

    /** Timeout : les réservations non versées dans le délai expirent (fonds libérés, disbursement-failed). */
    public int expirerReservations() {
        List<Disbursement> expirees = disbursementRepository
                .findByStatutAndDateExpirationBefore(StatutDecaissement.RESERVE, LocalDateTime.now());
        for (Disbursement d : expirees) {
            liberer(trouver(d.getId()), StatutDecaissement.ANNULE, "Délai de versement dépassé : réservation expirée", null);
        }
        return expirees.size();
    }

    // ============================ Outils ============================

    /** Compensation : rend les fonds réservés disponibles et publie disbursement-failed. */
    private Disbursement liberer(Disbursement d, StatutDecaissement statut, String motif, String causationId) {
        Long id = d.getId();
        BigDecimal libere = BigDecimal.ZERO;
        if (d.getStatut() == StatutDecaissement.RESERVE && d.getTreasuryAccount() != null) {
            if (compteRepository.liberer(d.getTreasuryAccount().getId(), d.getMontant()) == 1) {
                libere = d.getMontant();
            }
            d = trouver(id); // contexte de persistance vidé par la requête de mise à jour
        }
        d.setStatut(statut);
        d.setMotifEchec(motif);
        disbursementRepository.saveAndFlush(d);
        publierEchec(d, libere, causationId);
        return d;
    }

    private void publierEchec(Disbursement d, BigDecimal fondsLiberes, String causationId) {
        Long compteId = d.getTreasuryAccount() == null ? null : d.getTreasuryAccount().getId();
        outbox.enregistrer(Topics.DISBURSEMENT_FAILED, Topics.TYPE_DISBURSEMENT_FAILED,
                String.valueOf(d.getLoanApplicationId()), d.getVersion(), d.getCorrelationId(), causationId,
                new VersementEchoueDonnees(d.getId(), d.getLoanApplicationId(), compteId, d.getMontant(),
                        d.getStatut().name(), d.getMotifEchec(), fondsLiberes));
    }

    private Disbursement trouver(Long id) {
        return disbursementRepository.findById(id)
                .orElseThrow(() -> new RessourceIntrouvableException("Décaissement " + id + " introuvable"));
    }
}
