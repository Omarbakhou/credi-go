package com.credigo.credit.messaging;

import com.fasterxml.jackson.databind.JsonNode;

import java.time.Instant;

/**
 * Enveloppe commune à tous les événements Kafka de Credi-Go (V7), sérialisée en JSON.
 * <ul>
 *   <li>eventId : identifiant unique de l'événement, clé d'idempotence des consommateurs ;</li>
 *   <li>eventType : type métier (APPLICATION_DEPOSITED, LOAN_APPROVED, ...) ;</li>
 *   <li>version : version de l'agrégat au moment de l'émission (détection des résultats périmés) ;</li>
 *   <li>correlationId : identifiant du processus métier, identique sur tout le cycle d'un dossier ;</li>
 *   <li>causationId : eventId de l'événement qui a provoqué celui-ci (null pour une action utilisateur) ;</li>
 *   <li>businessId : identifiant métier de l'agrégat (dossier, client, décaissement), aussi clé Kafka ;</li>
 *   <li>source : application émettrice ; occurredAt : date d'émission ;</li>
 *   <li>data : données nécessaires au traitement par les consommateurs.</li>
 * </ul>
 */
public record EvenementMetier(
        String eventId,
        String eventType,
        long version,
        String correlationId,
        String causationId,
        String businessId,
        String source,
        Instant occurredAt,
        JsonNode data
) {
}
