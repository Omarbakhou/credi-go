package com.credigo.credit.projection;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Projection locale des décaissements d'un dossier, alimentée par disbursement-paid / disbursement-failed. */
@Entity
@Table(name = "decaissement_projection")
public class DecaissementProjection {

    @Id
    private Long decaissementId;

    @Column(nullable = false)
    private Long dossierId;

    private Long treasuryAccountId;

    @Column(precision = 14, scale = 2)
    private BigDecimal montant;

    private String statut;

    private LocalDateTime date;

    @Column(length = 500)
    private String motif;

    public DecaissementProjection() {
    }

    public DecaissementProjection(Long decaissementId) {
        this.decaissementId = decaissementId;
    }

    public Long getDecaissementId() { return decaissementId; }

    public Long getDossierId() { return dossierId; }
    public void setDossierId(Long dossierId) { this.dossierId = dossierId; }

    public Long getTreasuryAccountId() { return treasuryAccountId; }
    public void setTreasuryAccountId(Long treasuryAccountId) { this.treasuryAccountId = treasuryAccountId; }

    public BigDecimal getMontant() { return montant; }
    public void setMontant(BigDecimal montant) { this.montant = montant; }

    public String getStatut() { return statut; }
    public void setStatut(String statut) { this.statut = statut; }

    public LocalDateTime getDate() { return date; }
    public void setDate(LocalDateTime date) { this.date = date; }

    public String getMotif() { return motif; }
    public void setMotif(String motif) { this.motif = motif; }
}
