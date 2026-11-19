package com.credigo.decaissements.entite;

import com.credigo.decaissements.enums.StatutDecaissement;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Versement d'un crédit. loanApplicationId référence un dossier du service-credit
 * (simple identifiant, sans association JPA inter-services) ; le compte de trésorerie
 * appartient au même service (association locale).
 * Création et cycle de vie implémentés au jalon V4.
 */
@Entity
@Table(name = "disbursement")
public class Disbursement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long loanApplicationId;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "treasury_account_id", nullable = false)
    private TreasuryAccount treasuryAccount;

    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal montant;

    @Column(name = "date_versement", nullable = false)
    private LocalDateTime date;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StatutDecaissement statut;

    public Disbursement() {
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getLoanApplicationId() { return loanApplicationId; }
    public void setLoanApplicationId(Long loanApplicationId) { this.loanApplicationId = loanApplicationId; }

    public TreasuryAccount getTreasuryAccount() { return treasuryAccount; }
    public void setTreasuryAccount(TreasuryAccount treasuryAccount) { this.treasuryAccount = treasuryAccount; }

    public BigDecimal getMontant() { return montant; }
    public void setMontant(BigDecimal montant) { this.montant = montant; }

    public LocalDateTime getDate() { return date; }
    public void setDate(LocalDateTime date) { this.date = date; }

    public StatutDecaissement getStatut() { return statut; }
    public void setStatut(StatutDecaissement statut) { this.statut = statut; }
}
