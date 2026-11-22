package com.credigo.notification.messaging;

import jakarta.persistence.*;

import java.time.LocalDateTime;

/**
 * Événement déjà consommé (consommateur idempotent) : un eventId reçu une seconde fois
 * (rejeu Kafka, redémarrage avant commit de l'offset) est ignoré.
 * resultat trace la décision prise (traité, ou ignoré avec le motif, ex. résultat tardif).
 */
@Entity
@Table(name = "evenement_traite")
public class EvenementTraite {

    @Id
    private String eventId;

    @Column(nullable = false)
    private String eventType;

    private String topic;

    private String correlationId;

    private String businessId;

    @Column(length = 500)
    private String resultat;

    @Column(nullable = false)
    private LocalDateTime dateTraitement;

    protected EvenementTraite() {
    }

    public EvenementTraite(EvenementMetier e, String topic, String resultat) {
        this.eventId = e.eventId();
        this.eventType = e.eventType();
        this.topic = topic;
        this.correlationId = e.correlationId();
        this.businessId = e.businessId();
        this.resultat = resultat == null ? null : resultat.substring(0, Math.min(500, resultat.length()));
        this.dateTraitement = LocalDateTime.now();
    }

    public String getEventId() { return eventId; }
    public String getEventType() { return eventType; }
    public String getTopic() { return topic; }
    public String getCorrelationId() { return correlationId; }
    public String getBusinessId() { return businessId; }
    public String getResultat() { return resultat; }
    public LocalDateTime getDateTraitement() { return dateTraitement; }
}
