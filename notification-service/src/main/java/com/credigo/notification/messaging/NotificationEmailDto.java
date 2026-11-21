package com.credigo.notification.messaging;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/** Message reçu depuis RabbitMQ (même structure JSON que côté publishers). */
public record NotificationEmailDto(
        @NotBlank String evenement,
        Long dossierId,
        @NotBlank @Email String destinataire,
        @NotBlank String objet,
        @NotBlank String contenu
) {
}
