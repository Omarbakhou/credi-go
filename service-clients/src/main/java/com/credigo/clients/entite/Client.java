package com.credigo.clients.entite;

import com.credigo.clients.enums.StatutClient;
import jakarta.persistence.*;
import org.hibernate.annotations.ColumnDefault;

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

    /**
     * Version des événements client-events (V7), strictement croissante : max(version + 1, horodatage).
     * L'horodatage garantit qu'un état republié après un redémarrage (base H2 réinitialisée)
     * reste plus récent que les événements déjà présents dans Kafka.
     */
    @ColumnDefault("0")
    @Column(name = "version_client")
    private long version;

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

    public long getVersion() { return version; }

    /** Nouvelle version avant chaque publication d'un événement client. */
    public long incrementerVersion() {
        version = Math.max(version + 1, System.currentTimeMillis());
        return version;
    }
}
