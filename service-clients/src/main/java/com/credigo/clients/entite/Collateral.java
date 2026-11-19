package com.credigo.clients.entite;

import com.credigo.clients.enums.TypeGarantie;
import jakarta.persistence.*;

import java.math.BigDecimal;

@Entity
@Table(name = "collateral")
public class Collateral {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Référence locale (même service) : association JPA autorisée. */
    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "client_id", nullable = false)
    private Client client;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TypeGarantie type;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal valeur;

    private String description;

    public Collateral() {
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Client getClient() { return client; }
    public void setClient(Client client) { this.client = client; }

    public TypeGarantie getType() { return type; }
    public void setType(TypeGarantie type) { this.type = type; }

    public BigDecimal getValeur() { return valeur; }
    public void setValeur(BigDecimal valeur) { this.valeur = valeur; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
}
