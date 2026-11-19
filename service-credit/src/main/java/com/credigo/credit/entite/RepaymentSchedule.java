package com.credigo.credit.entite;

import com.credigo.credit.enums.StatutEcheance;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Ligne d'échéancier : association locale au dossier (même service). */
@Entity
@Table(name = "repayment_schedule")
public class RepaymentSchedule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "loan_application_id", nullable = false)
    private LoanApplication loanApplication;

    @Column(nullable = false)
    private int numeroEcheance;

    @Column(nullable = false)
    private LocalDate dateEcheance;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal montantCapital;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal montantInterets;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StatutEcheance statut;

    public RepaymentSchedule() {
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public LoanApplication getLoanApplication() { return loanApplication; }
    public void setLoanApplication(LoanApplication loanApplication) { this.loanApplication = loanApplication; }

    public int getNumeroEcheance() { return numeroEcheance; }
    public void setNumeroEcheance(int numeroEcheance) { this.numeroEcheance = numeroEcheance; }

    public LocalDate getDateEcheance() { return dateEcheance; }
    public void setDateEcheance(LocalDate dateEcheance) { this.dateEcheance = dateEcheance; }

    public BigDecimal getMontantCapital() { return montantCapital; }
    public void setMontantCapital(BigDecimal montantCapital) { this.montantCapital = montantCapital; }

    public BigDecimal getMontantInterets() { return montantInterets; }
    public void setMontantInterets(BigDecimal montantInterets) { this.montantInterets = montantInterets; }

    public StatutEcheance getStatut() { return statut; }
    public void setStatut(StatutEcheance statut) { this.statut = statut; }
}
