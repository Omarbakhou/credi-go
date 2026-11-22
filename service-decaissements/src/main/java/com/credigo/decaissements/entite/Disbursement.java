package com.credigo.decaissements.entite;

import com.credigo.decaissements.enums.StatutDecaissement;
import jakarta.persistence.*;
import org.hibernate.annotations.ColumnDefault;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Versement d'un crédit. loanApplicationId et clientId référencent des données d'autres services
 * (simples identifiants, sans association JPA inter-services) ; le compte de trésorerie appartient
 * au même service (association locale).
 * V7 : RESERVE (préparé à la réception de loan-approved, fonds réservés) -> VERSE (disbursement-paid),
 * ou -> ANNULE / ECHOUE (fonds libérés, disbursement-failed).
 */
@Entity
@Table(name = "disbursement")
public class Disbursement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long loanApplicationId;

    private Long clientId;

    /** Compte financeur ; null si aucun compte n'avait un solde suffisant (décaissement ECHOUE). */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "treasury_account_id")
    private TreasuryAccount treasuryAccount;

    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal montant;

    /** Date du versement effectif. */
    @Column(name = "date_versement")
    private LocalDateTime date;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StatutDecaissement statut;

    /** Processus du dossier (même correlationId que les événements de service-credit). */
    private String correlationId;

    /** Tentative de décaissement du dossier (loan-approved.tentative). */
    @ColumnDefault("1")
    private int tentative = 1;

    private LocalDateTime dateReservation;

    /** Au-delà, une réservation non versée expire (fonds libérés). */
    private LocalDateTime dateExpiration;

    @Column(length = 500)
    private String motifEchec;

    @Version
    @ColumnDefault("0")
    @Column(name = "version_decaissement")
    private Long version;

    public Disbursement() {
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getLoanApplicationId() { return loanApplicationId; }
    public void setLoanApplicationId(Long loanApplicationId) { this.loanApplicationId = loanApplicationId; }

    public Long getClientId() { return clientId; }
    public void setClientId(Long clientId) { this.clientId = clientId; }

    public TreasuryAccount getTreasuryAccount() { return treasuryAccount; }
    public void setTreasuryAccount(TreasuryAccount treasuryAccount) { this.treasuryAccount = treasuryAccount; }

    public BigDecimal getMontant() { return montant; }
    public void setMontant(BigDecimal montant) { this.montant = montant; }

    public LocalDateTime getDate() { return date; }
    public void setDate(LocalDateTime date) { this.date = date; }

    public StatutDecaissement getStatut() { return statut; }
    public void setStatut(StatutDecaissement statut) { this.statut = statut; }

    public String getCorrelationId() { return correlationId; }
    public void setCorrelationId(String correlationId) { this.correlationId = correlationId; }

    public int getTentative() { return tentative; }
    public void setTentative(int tentative) { this.tentative = tentative; }

    public LocalDateTime getDateReservation() { return dateReservation; }
    public void setDateReservation(LocalDateTime dateReservation) { this.dateReservation = dateReservation; }

    public LocalDateTime getDateExpiration() { return dateExpiration; }
    public void setDateExpiration(LocalDateTime dateExpiration) { this.dateExpiration = dateExpiration; }

    public String getMotifEchec() { return motifEchec; }
    public void setMotifEchec(String motifEchec) { this.motifEchec = motifEchec; }

    public Long getVersion() { return version; }
}
