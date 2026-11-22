package com.credigo.notification.messaging;

import com.credigo.notification.service.NotificationService;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * Consommateurs Kafka (groupe notification-service), idempotents par eventId : un événement
 * redélivré ne provoque pas un second email.
 */
@Component
public class NotificationConsumer {

    private final ConsommateurIdempotent consommateur;
    private final NotificationService notificationService;

    public NotificationConsumer(ConsommateurIdempotent consommateur, NotificationService notificationService) {
        this.consommateur = consommateur;
        this.notificationService = notificationService;
    }

    @KafkaListener(topics = Topics.LOAN_APPROVED)
    public void surCreditAccorde(String message) {
        consommateur.traiter(Topics.LOAN_APPROVED, message, notificationService::notifierAccord);
    }

    @KafkaListener(topics = Topics.DISBURSEMENT_PAID)
    public void surVersementEffectue(String message) {
        consommateur.traiter(Topics.DISBURSEMENT_PAID, message, notificationService::notifierVersement);
    }
}
