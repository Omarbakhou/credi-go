package com.credigo.credit.entite;

import com.credigo.credit.enums.StatutDossier;
import jakarta.persistence.*;
import org.hibernate.annotations.ColumnDefault;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Dossier de crédit (unité de traitement). clientId référence un client du service-clients :
 * simple identifiant, sans association JPA inter-services.
 */
@Entity
@Table(name = "loan_application")
public class LoanApplication {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Clé d'idempotence fournie par l'appelant (en-tête Idempotency-Key, V6). */
    @Column(unique = true)
    private String idempotencyKey;

    @Column(nullable = false)
    private Long clientId;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal montantDemande;

    @Column(precision = 12, scale = 2)
    private BigDecimal montantAccorde;

    private Integer scoreEtude;

    @Column(nullable = false)
    private int dureeMois;

    /** Taux annuel proposé, en pourcentage (ex. 6.50). */
    @Column(precision = 5, scale = 2)
    private BigDecimal tauxPropose;

    /** Objet du crédit (ex. achat véhicule). */
    private String motif;

    /** Justification de la décision (accord total/partiel ou motif de refus). */
    private String commentaireDecision;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StatutDossier statut;

    /** Verrou optimiste ; aussi version de la demande transmise au scoring (un score d'une version périmée est ignoré). */
    @Version
    @ColumnDefault("0")
    @Column(name = "version_dossier")
    private Long version;

    /** Identifiant de corrélation du processus (V7) : porté par tous les événements du dossier. */
    private String correlationId;

    private LocalDateTime dateDepot;

    /** Numéro de la dernière tentative de décaissement demandée (loan-approved). */
    @ColumnDefault("0")
    private int tentativeDecaissement;

    /** Vrai après un disbursement-failed : le dossier reste ACCORDE, décaissement à relancer. */
    @ColumnDefault("false")
    private boolean decaissementAReprendre;

    private String motifEchecDecaissement;

    @OneToMany(mappedBy = "loanApplication", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("numeroEcheance ASC")
    private List<RepaymentSchedule> echeancier = new ArrayList<>();

    public LoanApplication() {
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getIdempotencyKey() { return idempotencyKey; }
    public void setIdempotencyKey(String idempotencyKey) { this.idempotencyKey = idempotencyKey; }

    public Long getClientId() { return clientId; }
    public void setClientId(Long clientId) { this.clientId = clientId; }

    public BigDecimal getMontantDemande() { return montantDemande; }
    public void setMontantDemande(BigDecimal montantDemande) { this.montantDemande = montantDemande; }

    public BigDecimal getMontantAccorde() { return montantAccorde; }
    public void setMontantAccorde(BigDecimal montantAccorde) { this.montantAccorde = montantAccorde; }

    public Integer getScoreEtude() { return scoreEtude; }
    public void setScoreEtude(Integer scoreEtude) { this.scoreEtude = scoreEtude; }

    public int getDureeMois() { return dureeMois; }
    public void setDureeMois(int dureeMois) { this.dureeMois = dureeMois; }

    public BigDecimal getTauxPropose() { return tauxPropose; }
    public void setTauxPropose(BigDecimal tauxPropose) { this.tauxPropose = tauxPropose; }

    public String getMotif() { return motif; }
    public void setMotif(String motif) { this.motif = motif; }

    public String getCommentaireDecision() { return commentaireDecision; }
    public void setCommentaireDecision(String commentaireDecision) { this.commentaireDecision = commentaireDecision; }

    public StatutDossier getStatut() { return statut; }
    public void setStatut(StatutDossier statut) { this.statut = statut; }

    public List<RepaymentSchedule> getEcheancier() { return echeancier; }

    public Long getVersion() { return version; }

    public String getCorrelationId() { return correlationId; }
    public void setCorrelationId(String correlationId) { this.correlationId = correlationId; }

    public LocalDateTime getDateDepot() { return dateDepot; }
    public void setDateDepot(LocalDateTime dateDepot) { this.dateDepot = dateDepot; }

    public int getTentativeDecaissement() { return tentativeDecaissement; }
    public void setTentativeDecaissement(int tentativeDecaissement) { this.tentativeDecaissement = tentativeDecaissement; }

    public boolean isDecaissementAReprendre() { return decaissementAReprendre; }
    public void setDecaissementAReprendre(boolean decaissementAReprendre) { this.decaissementAReprendre = decaissementAReprendre; }

    public String getMotifEchecDecaissement() { return motifEchecDecaissement; }
    public void setMotifEchecDecaissement(String motifEchecDecaissement) { this.motifEchecDecaissement = motifEchecDecaissement; }
}
