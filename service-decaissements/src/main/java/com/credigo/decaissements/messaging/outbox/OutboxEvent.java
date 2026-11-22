package com.credigo.decaissements.messaging.outbox;

import jakarta.persistence.*;

import java.time.LocalDateTime;

/**
 * Transactional outbox : l'événement est écrit dans cette table dans la MÊME transaction que
 * l'opération métier. Il n'existe donc que si l'opération est validée (commit), et il sera
 * publié dans Kafka par {@link OutboxPublisher}, avec nouvel essai tant que Kafka ne l'a pas accepté.
 */
@Entity
@Table(name = "outbox_event")
public class OutboxEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String eventId;

    @Column(nullable = false)
    private String eventType;

    @Column(nullable = false)
    private String topic;

    /** Clé Kafka (identifiant métier) : garantit l'ordre des événements d'un même agrégat. */
    @Column(nullable = false)
    private String cle;

    private String correlationId;

    /** Enveloppe complète sérialisée en JSON. */
    @Lob
    @Column(nullable = false)
    private String payload;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StatutOutbox statut;

    private int tentatives;

    @Column(length = 500)
    private String derniereErreur;

    @Column(nullable = false)
    private LocalDateTime dateCreation;

    private LocalDateTime datePublication;

    public OutboxEvent() {
    }

    public void marquerPublie() {
        tentatives++;
        statut = StatutOutbox.PUBLIE;
        datePublication = LocalDateTime.now();
        derniereErreur = null;
    }

    public void marquerEchec(String erreur) {
        tentatives++;
        derniereErreur = erreur == null ? null : erreur.substring(0, Math.min(500, erreur.length()));
    }

    public Long getId() { return id; }

    public String getEventId() { return eventId; }
    public void setEventId(String eventId) { this.eventId = eventId; }

    public String getEventType() { return eventType; }
    public void setEventType(String eventType) { this.eventType = eventType; }

    public String getTopic() { return topic; }
    public void setTopic(String topic) { this.topic = topic; }

    public String getCle() { return cle; }
    public void setCle(String cle) { this.cle = cle; }

    public String getCorrelationId() { return correlationId; }
    public void setCorrelationId(String correlationId) { this.correlationId = correlationId; }

    public String getPayload() { return payload; }
    public void setPayload(String payload) { this.payload = payload; }

    public StatutOutbox getStatut() { return statut; }
    public void setStatut(StatutOutbox statut) { this.statut = statut; }

    public int getTentatives() { return tentatives; }

    public String getDerniereErreur() { return derniereErreur; }

    public LocalDateTime getDateCreation() { return dateCreation; }
    public void setDateCreation(LocalDateTime dateCreation) { this.dateCreation = dateCreation; }

    public LocalDateTime getDatePublication() { return datePublication; }
}
