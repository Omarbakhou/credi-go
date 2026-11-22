package com.credigo.decaissements.messaging;

import com.credigo.decaissements.service.DecaissementService;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/** Consommateur Kafka de service-decaissements (groupe service-decaissements), idempotent par eventId. */
@Component
public class DecaissementsEventsListener {

    private final ConsommateurIdempotent consommateur;
    private final DecaissementService decaissementService;

    public DecaissementsEventsListener(ConsommateurIdempotent consommateur, DecaissementService decaissementService) {
        this.consommateur = consommateur;
        this.decaissementService = decaissementService;
    }

    /** Crédit accordé : préparation du décaissement et réservation des fonds. */
    @KafkaListener(topics = Topics.LOAN_APPROVED)
    public void surCreditAccorde(String message) {
        consommateur.traiter(Topics.LOAN_APPROVED, message, decaissementService::surCreditAccorde);
    }
}
