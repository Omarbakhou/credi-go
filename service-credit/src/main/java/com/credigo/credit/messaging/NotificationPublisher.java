package com.credigo.credit.messaging;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * Publisher RabbitMQ. La publication est différée APRÈS le commit de la transaction :
 * aucun email n'est envoyé pour une opération finalement annulée (rollback).
 */
@Component
public class NotificationPublisher {

    private static final Logger log = LoggerFactory.getLogger(NotificationPublisher.class);
    private final RabbitTemplate rabbitTemplate;

    public NotificationPublisher(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    public void publierApresCommit(NotificationEmailDto notification) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    publier(notification);
                }
            });
        } else {
            publier(notification);
        }
    }

    private void publier(NotificationEmailDto notification) {
        try {
            rabbitTemplate.convertAndSend(RabbitConfig.QUEUE_EMAIL, notification);
            log.info("Événement {} publié pour le dossier {}", notification.evenement(), notification.dossierId());
        } catch (Exception e) {
            // L'opération métier est déjà validée : l'échec de notification ne doit pas l'annuler
            log.error("Publication RabbitMQ impossible ({}) : {}", notification.evenement(), e.getMessage());
        }
    }
}
