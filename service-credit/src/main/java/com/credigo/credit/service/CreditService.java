package com.credigo.credit.service;

import com.credigo.credit.dto.*;
import com.credigo.credit.entite.LoanApplication;
import com.credigo.credit.entite.RepaymentSchedule;
import com.credigo.credit.enums.StatutDossier;
import com.credigo.credit.enums.StatutEcheance;
import com.credigo.credit.exception.RegleMetierException;
import com.credigo.credit.exception.RessourceIntrouvableException;
import com.credigo.credit.exception.ServiceExterneIndisponibleException;
import com.credigo.credit.mapper.CreditMapper;
import com.credigo.credit.messaging.ConsommateurIdempotent;
import com.credigo.credit.messaging.EvenementMetier;
import com.credigo.credit.messaging.Topics;
import com.credigo.credit.messaging.donnees.*;
import com.credigo.credit.messaging.outbox.OutboxService;
import com.credigo.credit.projection.*;
import com.credigo.credit.repository.LoanApplicationRepository;
import com.credigo.credit.repository.RepaymentScheduleRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Cycle de vie du dossier de crédit, orchestré par événements Kafka (V7) :
 * <pre>
 * POST /credits            DEPOSE      --application-deposited-->  service-clients (scoring)
 * client-scored            EN_ETUDE    (score de la version courante de la demande)
 * POST /credits/{id}/decision ACCORDE  --loan-approved-->          service-decaissements, notification-service
 * disbursement-paid        DEBLOQUE    (échéancier activé)
 * disbursement-failed      ACCORDE     (décaissement à reprendre, échéancier inactif)
 * </pre>
 * Chaque service décide sur ses propres données : les références au client (existence, statut,
 * revenus, garanties) sont lues dans la projection locale alimentée par client-events.
 * Les événements sont écrits dans l'outbox dans la même transaction que le changement d'état.
 */
@Service
@Transactional
public class CreditService {

    private final LoanApplicationRepository dossierRepository;
    private final RepaymentScheduleRepository echeanceRepository;
    private final CreditMapper mapper;
    private final ProjectionClientService projectionClients;
    private final DecaissementProjectionRepository projectionDecaissements;
    private final OutboxService outbox;
    private final ConsommateurIdempotent lecteur;

    public CreditService(LoanApplicationRepository dossierRepository,
                         RepaymentScheduleRepository echeanceRepository, CreditMapper mapper,
                         ProjectionClientService projectionClients,
                         DecaissementProjectionRepository projectionDecaissements,
                         OutboxService outbox, ConsommateurIdempotent lecteur) {
        this.dossierRepository = dossierRepository;
        this.echeanceRepository = echeanceRepository;
        this.mapper = mapper;
        this.projectionClients = projectionClients;
        this.projectionDecaissements = projectionDecaissements;
        this.outbox = outbox;
        this.lecteur = lecteur;
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

    /** Consultation enrichie composée uniquement à partir des données et projections locales. */
    @Transactional(readOnly = true)
    public DossierDetailDto consulterDetail(Long id) {
        LoanApplication dossier = trouver(id);
        ClientProjeteDto client = projectionClients.client(dossier.getClientId())
                .map(ProjectionClientService::toDto).orElse(null);
        List<GarantieProjeteeDto> garanties = projectionClients.garanties(dossier.getClientId());
        List<DecaissementProjeteDto> decaissements = projectionDecaissements
                .findByDossierIdOrderByDecaissementId(id).stream()
                .map(d -> new DecaissementProjeteDto(d.getDecaissementId(), d.getDossierId(), d.getTreasuryAccountId(),
                        d.getMontant(), d.getDate(), d.getStatut(), d.getMotif()))
                .toList();
        List<RepaymentScheduleDto> echeancier = dossier.getEcheancier().stream().map(mapper::toDto).toList();
        return new DossierDetailDto(mapper.toDto(dossier), client, garanties, echeancier, decaissements,
                client == null);
    }

    // ============================ Dépôt et modification ============================

    /**
     * Dépôt d'une demande : le client est vérifié dans la projection locale (404 inconnu, 409 inactif),
     * puis application-deposited est écrit dans l'outbox. Idempotent (V6) par l'en-tête Idempotency-Key.
     */
    public ResultatDepot deposer(DemandeCreditDto dto, String idempotencyKey) {
        String cle = normaliserCle(idempotencyKey);
        if (cle != null) {
            Optional<LoanApplication> existant = dossierRepository.findByIdempotencyKey(cle);
            if (existant.isPresent()) {
                return new ResultatDepot(mapper.toDto(existant.get()), false);
            }
        }
        verifierClientActif(dto.clientId());

        LoanApplication dossier = new LoanApplication();
        mapper.copierDemande(dto, dossier);
        dossier.setIdempotencyKey(cle);
        dossier.setStatut(StatutDossier.DEPOSE);
        dossier.setDateDepot(LocalDateTime.now());
        dossier.setCorrelationId(UUID.randomUUID().toString());
        dossierRepository.saveAndFlush(dossier);
        publierDemandeDeposee(dossier);
        return new ResultatDepot(mapper.toDto(dossier), true);
    }

    /** Modification d'une demande DEPOSE : nouvelle version, donc nouveau scoring (l'ancien sera ignoré). */
    public LoanApplicationDto modifier(Long id, DemandeCreditDto dto) {
        LoanApplication dossier = trouver(id);
        exigerStatut(dossier, StatutDossier.DEPOSE, "modifié");
        if (!dossier.getClientId().equals(dto.clientId())) {
            verifierClientActif(dto.clientId());
        }
        mapper.copierDemande(dto, dossier);
        dossierRepository.saveAndFlush(dossier); // incrémente la version de la demande
        publierDemandeDeposee(dossier);
        return mapper.toDto(dossier);
    }

    public void supprimer(Long id) {
        LoanApplication dossier = trouver(id);
        StatutDossier s = dossier.getStatut();
        if (s != StatutDossier.DEPOSE && s != StatutDossier.REFUSE && s != StatutDossier.ANNULE) {
            throw new RegleMetierException("Suppression impossible : le dossier " + id + " est " + s);
        }
        dossierRepository.delete(dossier);
    }

    /** Annulation avant décision : un score qui arriverait ensuite ne réactive pas le dossier. */
    public LoanApplicationDto annuler(Long id) {
        LoanApplication dossier = trouver(id);
        if (dossier.getStatut() != StatutDossier.DEPOSE && dossier.getStatut() != StatutDossier.EN_ETUDE) {
            throw new RegleMetierException("Le dossier " + id + " est " + dossier.getStatut()
                    + " : seule une demande DEPOSE ou EN_ETUDE peut être annulée");
        }
        dossier.setStatut(StatutDossier.ANNULE);
        dossier.setCommentaireDecision("Demande annulée avant décision");
        return mapper.toDto(dossier);
    }

    /** Délai d'étude dépassé (aucun client-scored reçu) : la demande est annulée. */
    public int expirerEtudes(LocalDateTime limite) {
        List<LoanApplication> expires = dossierRepository.findByStatutAndDateDepotBefore(StatutDossier.DEPOSE, limite);
        expires.forEach(d -> {
            d.setStatut(StatutDossier.ANNULE);
            d.setCommentaireDecision("Délai d'étude dépassé : aucun score reçu de service-clients");
        });
        return expires.size();
    }

    // ============================ client-scored ============================

    /** client-scored : DEPOSE -> EN_ETUDE, uniquement pour la version courante d'une demande encore DEPOSE. */
    public String surClientScore(EvenementMetier e) {
        ClientScoreDonnees score = lecteur.donnees(e, ClientScoreDonnees.class);
        Optional<LoanApplication> trouve = dossierRepository.findById(score.dossierId());
        if (trouve.isEmpty()) {
            return "IGNORE : dossier " + score.dossierId() + " inexistant (supprimé)";
        }
        LoanApplication dossier = trouve.get();
        if (dossier.getStatut() != StatutDossier.DEPOSE) {
            return "IGNORE : résultat tardif, le dossier " + dossier.getId() + " est " + dossier.getStatut();
        }
        if (dossier.getVersion() != score.versionDossier()) {
            return "IGNORE : score de la version " + score.versionDossier() + " périmé (demande en version "
                    + dossier.getVersion() + ")";
        }
        if (!score.clientEligible()) {
            refuser(dossier, score.motif());
            return "REFUSE : " + score.motif();
        }
        dossier.setScoreEtude(score.score());
        dossier.setStatut(StatutDossier.EN_ETUDE);
        return "EN_ETUDE (score " + score.score() + ")";
    }

    // ============================ Décision ============================

    /**
     * EN_ETUDE -> ACCORDE ou REFUSE selon {@link RegleOctroi}, sur les données locales (score reçu,
     * projection du client). En cas d'accord : échéancier INACTIF et loan-approved dans l'outbox.
     */
    public LoanApplicationDto decider(Long id) {
        LoanApplication dossier = trouver(id);
        exigerStatut(dossier, StatutDossier.EN_ETUDE, "décidé");
        ClientProjection client = projectionClients.client(dossier.getClientId())
                .orElseThrow(() -> new ServiceExterneIndisponibleException("Client " + dossier.getClientId()
                        + " absent de la projection locale : décision impossible pour l'instant"
                        + " (POST /projections/clients/reconstruction)"));

        int score = dossier.getScoreEtude() == null ? 0 : dossier.getScoreEtude();
        BigDecimal taux = RegleOctroi.tauxPourScore(score);
        if (!client.estActif()) {
            return mapper.toDto(refuser(dossier, "Client inactif"));
        }
        if (taux == null) {
            return mapper.toDto(refuser(dossier, "Score insuffisant (" + score + " < " + RegleOctroi.SCORE_MINIMUM + ")"));
        }

        BigDecimal capacite = client.getRevenusMensuels().multiply(RegleOctroi.TAUX_ENDETTEMENT_MAX);
        BigDecimal montantAccorde = dossier.getMontantDemande();
        String commentaire = "Accord total";
        if (RegleOctroi.premiereMensualite(montantAccorde, dossier.getDureeMois(), taux).compareTo(capacite) > 0) {
            montantAccorde = RegleOctroi.montantMaximal(capacite, dossier.getDureeMois(), taux);
            BigDecimal moitie = dossier.getMontantDemande().divide(new BigDecimal("2"), 2, RoundingMode.HALF_UP);
            if (montantAccorde.compareTo(moitie) < 0) {
                return mapper.toDto(refuser(dossier, "Taux d'endettement supérieur à 40 % des revenus"));
            }
            commentaire = "Accord partiel limité par le taux d'endettement (40 % des revenus)";
        }
        if (montantAccorde.compareTo(RegleOctroi.SEUIL_GARANTIES) > 0) {
            BigDecimal totalGaranties = projectionClients.garanties(dossier.getClientId()).stream()
                    .map(GarantieProjeteeDto::valeur).reduce(BigDecimal.ZERO, BigDecimal::add);
            if (totalGaranties.compareTo(montantAccorde.multiply(RegleOctroi.COUVERTURE_GARANTIES)) < 0) {
                return mapper.toDto(refuser(dossier, "Garanties insuffisantes (moins de 30 % du montant)"));
            }
        }

        dossier.setMontantAccorde(montantAccorde);
        dossier.setTauxPropose(taux);
        dossier.setCommentaireDecision(commentaire);
        dossier.setStatut(StatutDossier.ACCORDE);
        genererEcheancier(dossier);
        dossier.setTentativeDecaissement(1);
        dossierRepository.saveAndFlush(dossier);
        publierCreditAccorde(dossier, client);
        return mapper.toDto(dossier);
    }

    /** Après un disbursement-failed : nouvelle demande de décaissement (loan-approved, tentative suivante). */
    public LoanApplicationDto relancerDecaissement(Long id) {
        LoanApplication dossier = trouver(id);
        exigerStatut(dossier, StatutDossier.ACCORDE, "relancé");
        if (!dossier.isDecaissementAReprendre()) {
            throw new RegleMetierException("Le décaissement du dossier " + id
                    + " n'est pas à reprendre (en cours ou non échoué)");
        }
        ClientProjection client = projectionClients.client(dossier.getClientId())
                .orElseThrow(() -> new ServiceExterneIndisponibleException("Client " + dossier.getClientId()
                        + " absent de la projection locale"));
        dossier.setDecaissementAReprendre(false);
        dossier.setMotifEchecDecaissement(null);
        dossier.setTentativeDecaissement(dossier.getTentativeDecaissement() + 1);
        dossierRepository.saveAndFlush(dossier);
        publierCreditAccorde(dossier, client);
        return mapper.toDto(dossier);
    }

    // ============================ Résultats du décaissement ============================

    /** disbursement-paid : ACCORDE -> DEBLOQUE et activation de l'échéancier (uniquement après versement). */
    public String surVersementEffectue(EvenementMetier e) {
        VersementEffectueDonnees v = lecteur.donnees(e, VersementEffectueDonnees.class);
        projeterDecaissement(v.decaissementId(), v.dossierId(), v.treasuryAccountId(), v.montant(), "VERSE",
                v.dateVersement(), null);
        Optional<LoanApplication> trouve = dossierRepository.findById(v.dossierId());
        if (trouve.isEmpty()) {
            return "IGNORE : dossier " + v.dossierId() + " inexistant";
        }
        LoanApplication dossier = trouve.get();
        if (dossier.getStatut() == StatutDossier.DEBLOQUE) {
            return "IGNORE : dossier " + dossier.getId() + " déjà DEBLOQUE";
        }
        if (dossier.getStatut() != StatutDossier.ACCORDE) {
            return "IGNORE : dossier " + dossier.getId() + " est " + dossier.getStatut();
        }
        dossier.setStatut(StatutDossier.DEBLOQUE);
        dossier.setDecaissementAReprendre(false);
        dossier.setMotifEchecDecaissement(null);
        dossier.getEcheancier().forEach(ech -> ech.setStatut(StatutEcheance.A_PAYER));
        return "DEBLOQUE : échéancier activé (" + dossier.getEcheancier().size() + " échéances A_PAYER)";
    }

    /** disbursement-failed : le dossier reste ACCORDE, échéancier inactif, décaissement à reprendre. */
    public String surVersementEchoue(EvenementMetier e) {
        VersementEchoueDonnees v = lecteur.donnees(e, VersementEchoueDonnees.class);
        if (v.decaissementId() != null) {
            projeterDecaissement(v.decaissementId(), v.dossierId(), v.treasuryAccountId(), v.montant(), v.statut(),
                    null, v.motif());
        }
        Optional<LoanApplication> trouve = dossierRepository.findById(v.dossierId());
        if (trouve.isEmpty()) {
            return "IGNORE : dossier " + v.dossierId() + " inexistant";
        }
        LoanApplication dossier = trouve.get();
        if (dossier.getStatut() != StatutDossier.ACCORDE) {
            // ex. échec tardif d'une ancienne tentative alors que le dossier est déjà DEBLOQUE
            return "IGNORE : échec tardif, le dossier " + dossier.getId() + " est " + dossier.getStatut();
        }
        dossier.setDecaissementAReprendre(true);
        dossier.setMotifEchecDecaissement(v.motif());
        return "ACCORDE : décaissement à reprendre (" + v.motif() + ")";
    }

    // ============================ Outils ============================

    private void publierDemandeDeposee(LoanApplication dossier) {
        outbox.enregistrer(Topics.APPLICATION_DEPOSITED, Topics.TYPE_APPLICATION_DEPOSITED,
                String.valueOf(dossier.getId()), dossier.getVersion(), dossier.getCorrelationId(), null,
                new DemandeDeposeeDonnees(dossier.getId(), dossier.getClientId(), dossier.getMontantDemande(),
                        dossier.getDureeMois(), dossier.getVersion()));
    }

    private void publierCreditAccorde(LoanApplication dossier, ClientProjection client) {
        outbox.enregistrer(Topics.LOAN_APPROVED, Topics.TYPE_LOAN_APPROVED, String.valueOf(dossier.getId()),
                dossier.getVersion(), dossier.getCorrelationId(), null,
                new CreditAccordeDonnees(dossier.getId(), dossier.getClientId(), client.getNom(), client.getEmail(),
                        dossier.getMontantAccorde(), dossier.getDureeMois(), dossier.getTauxPropose(),
                        dossier.getTentativeDecaissement()));
    }

    private void projeterDecaissement(Long decaissementId, Long dossierId, Long compteId, BigDecimal montant,
                                      String statut, LocalDateTime date, String motif) {
        DecaissementProjection p = projectionDecaissements.findById(decaissementId)
                .orElseGet(() -> new DecaissementProjection(decaissementId));
        if ("VERSE".equals(p.getStatut())) {
            return; // un versement effectif n'est jamais remplacé par un échec tardif
        }
        p.setDossierId(dossierId);
        p.setTreasuryAccountId(compteId);
        p.setMontant(montant);
        p.setStatut(statut);
        p.setDate(date);
        p.setMotif(motif);
        projectionDecaissements.save(p);
    }

    private LoanApplication refuser(LoanApplication dossier, String raison) {
        dossier.setStatut(StatutDossier.REFUSE);
        dossier.setCommentaireDecision(raison);
        return dossier;
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

    /** Référence distante vérifiée sur la projection locale : 404 si inconnu, 409 si inactif. */
    private void verifierClientActif(Long clientId) {
        ClientProjection client = projectionClients.client(clientId)
                .orElseThrow(() -> new RessourceIntrouvableException("Client " + clientId + " introuvable"));
        if (!client.estActif()) {
            throw new RegleMetierException("Le client " + clientId + " est inactif : opération refusée");
        }
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
