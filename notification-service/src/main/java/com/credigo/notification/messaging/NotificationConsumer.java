package com.credigo.notification.messaging;

import com.credigo.notification.service.NotificationService;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.util.Set;

/** Consumer RabbitMQ : chaque message de la file déclenche l'envoi d'un email. */
@Component
public class NotificationConsumer {

    private static final Logger log = LoggerFactory.getLogger(NotificationConsumer.class);

    private final NotificationService notificationService;
    private final Validator validator;

    public NotificationConsumer(NotificationService notificationService, Validator validator) {
        this.notificationService = notificationService;
        this.validator = validator;
    }

    @RabbitListener(queues = RabbitConfig.QUEUE_EMAIL)
    public void recevoir(NotificationEmailDto notification) {
        log.info("Message reçu de RabbitMQ : {} (dossier {})", notification.evenement(), notification.dossierId());
        Set<ConstraintViolation<NotificationEmailDto>> violations = validator.validate(notification);
        if (!violations.isEmpty()) {
            // Message invalide : rejeté sans être renvoyé dans la file (évite une boucle infinie)
            log.error("Message ignoré, invalide : {}", violations);
            return;
        }
        notificationService.envoyerEmail(notification);
    }
}
