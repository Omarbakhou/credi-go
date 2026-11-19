package com.credigo.decaissements.entite;

import jakarta.persistence.*;

import java.math.BigDecimal;

/** Enveloppe de trésorerie qui finance les décaissements. */
@Entity
@Table(name = "treasury_account")
public class TreasuryAccount {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String libelle;

    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal soldeDisponible;

    public TreasuryAccount() {
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getLibelle() { return libelle; }
    public void setLibelle(String libelle) { this.libelle = libelle; }

    public BigDecimal getSoldeDisponible() { return soldeDisponible; }
    public void setSoldeDisponible(BigDecimal soldeDisponible) { this.soldeDisponible = soldeDisponible; }
}
