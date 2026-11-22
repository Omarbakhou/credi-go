package com.credigo.decaissements.entite;

import jakarta.persistence.*;
import org.hibernate.annotations.ColumnDefault;

import java.math.BigDecimal;

/**
 * Enveloppe de trésorerie qui finance les décaissements.
 * V7 : soldeDisponible = fonds encore réservables ; montantReserve = fonds réservés pour des
 * décaissements préparés mais pas encore versés (libérés si le versement échoue ou expire).
 */
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

    @ColumnDefault("0")
    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal montantReserve = BigDecimal.ZERO;

    /** Verrou optimiste : les réservations atomiques (requêtes UPDATE) incrémentent aussi la version. */
    @Version
    @ColumnDefault("0")
    @Column(name = "version_compte")
    private Long version;

    public TreasuryAccount() {
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getLibelle() { return libelle; }
    public void setLibelle(String libelle) { this.libelle = libelle; }

    public BigDecimal getSoldeDisponible() { return soldeDisponible; }
    public void setSoldeDisponible(BigDecimal soldeDisponible) { this.soldeDisponible = soldeDisponible; }

    public BigDecimal getMontantReserve() { return montantReserve; }

    public Long getVersion() { return version; }
}
