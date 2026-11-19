package com.credigo.clients.entite;

import com.credigo.clients.enums.StatutClient;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "client")
public class Client {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String nom;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal revenusMensuels;

    /** Score interne du client (0 à 1000), recalculé lors de l'étude d'un dossier (V7). */
    private int scoreInterne;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StatutClient statut;

    /** Relation locale au service : les garanties appartiennent au même service. */
    @OneToMany(mappedBy = "client")
    private List<Collateral> garanties = new ArrayList<>();

    public Client() {
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getNom() { return nom; }
    public void setNom(String nom) { this.nom = nom; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public BigDecimal getRevenusMensuels() { return revenusMensuels; }
    public void setRevenusMensuels(BigDecimal revenusMensuels) { this.revenusMensuels = revenusMensuels; }

    public int getScoreInterne() { return scoreInterne; }
    public void setScoreInterne(int scoreInterne) { this.scoreInterne = scoreInterne; }

    public StatutClient getStatut() { return statut; }
    public void setStatut(StatutClient statut) { this.statut = statut; }

    public List<Collateral> getGaranties() { return garanties; }
}
