package com.credigo.decaissements.projection;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Projection locale d'un client de service-clients (V7), alimentée par le topic client-events.
 * service-decaissements ne lit jamais la base de service-clients : il décide sur cette copie locale.
 * version : version du client à l'émission ; un événement plus ancien que la projection est ignoré.
 * supprime : client supprimé côté service-clients (la ligne est conservée pour garder sa version).
 */
@Entity
@Table(name = "client_projection")
public class ClientProjection {

    @Id
    private Long clientId;

    private String nom;

    private String email;

    @Column(precision = 12, scale = 2)
    private BigDecimal revenusMensuels;

    private Integer scoreInterne;

    private String statut;

    private boolean supprime;

    private long version;

    private LocalDateTime dateMiseAJour;

    public ClientProjection() {
    }

    public ClientProjection(Long clientId) {
        this.clientId = clientId;
    }

    public boolean estActif() {
        return !supprime && "ACTIF".equals(statut);
    }

    public Long getClientId() { return clientId; }

    public String getNom() { return nom; }
    public void setNom(String nom) { this.nom = nom; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public BigDecimal getRevenusMensuels() { return revenusMensuels; }
    public void setRevenusMensuels(BigDecimal revenusMensuels) { this.revenusMensuels = revenusMensuels; }

    public Integer getScoreInterne() { return scoreInterne; }
    public void setScoreInterne(Integer scoreInterne) { this.scoreInterne = scoreInterne; }

    public String getStatut() { return statut; }
    public void setStatut(String statut) { this.statut = statut; }

    public boolean isSupprime() { return supprime; }
    public void setSupprime(boolean supprime) { this.supprime = supprime; }

    public long getVersion() { return version; }
    public void setVersion(long version) { this.version = version; }

    public LocalDateTime getDateMiseAJour() { return dateMiseAJour; }
    public void setDateMiseAJour(LocalDateTime dateMiseAJour) { this.dateMiseAJour = dateMiseAJour; }
}
