package com.credigo.decaissements.messaging;

/** Message publié dans RabbitMQ et consommé par le notification-service. */
public record NotificationEmailDto(
        String evenement,
        Long dossierId,
        String destinataire,
        String objet,
        String contenu
) {
}
