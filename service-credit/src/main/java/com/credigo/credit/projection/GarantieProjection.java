package com.credigo.credit.projection;

import jakarta.persistence.*;

import java.math.BigDecimal;

/** Projection locale d'une garantie : remplacée à chaque instantané du client propriétaire. */
@Entity
@Table(name = "garantie_projection")
public class GarantieProjection {

    @Id
    private Long id;

    @Column(nullable = false)
    private Long clientId;

    private String type;

    @Column(precision = 14, scale = 2)
    private BigDecimal valeur;

    private String description;

    public GarantieProjection() {
    }

    public GarantieProjection(Long id, Long clientId, String type, BigDecimal valeur, String description) {
        this.id = id;
        this.clientId = clientId;
        this.type = type;
        this.valeur = valeur;
        this.description = description;
    }

    public Long getId() { return id; }
    public Long getClientId() { return clientId; }
    public String getType() { return type; }
    public BigDecimal getValeur() { return valeur; }
    public String getDescription() { return description; }
}
