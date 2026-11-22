package com.credigo.notification.entite;

import jakarta.persistence.*;

import java.time.LocalDateTime;

/** Journal des notifications envoyées (V7 : persistant, consultable via GET /notifications). */
@Entity
@Table(name = "notification")
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private LocalDateTime date;

    /** Type de l'événement Kafka à l'origine de l'email (LOAN_APPROVED, DISBURSEMENT_PAID). */
    @Column(nullable = false)
    private String evenement;

    private Long dossierId;

    @Column(nullable = false)
    private String destinataire;

    @Column(nullable = false)
    private String objet;

    @Column(nullable = false)
    private String statut;

    private String eventId;

    private String correlationId;

    public Notification() {
    }

    public Notification(String evenement, Long dossierId, String destinataire, String objet, String statut,
                        String eventId, String correlationId) {
        this.date = LocalDateTime.now();
        this.evenement = evenement;
        this.dossierId = dossierId;
        this.destinataire = destinataire;
        this.objet = objet;
        this.statut = statut;
        this.eventId = eventId;
        this.correlationId = correlationId;
    }

    public Long getId() { return id; }
    public LocalDateTime getDate() { return date; }
    public String getEvenement() { return evenement; }
    public Long getDossierId() { return dossierId; }
    public String getDestinataire() { return destinataire; }
    public String getObjet() { return objet; }
    public String getStatut() { return statut; }
    public String getEventId() { return eventId; }
    public String getCorrelationId() { return correlationId; }
}
