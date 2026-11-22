package com.credigo.decaissements.messaging.outbox;

/** EN_ATTENTE : écrit avec l'opération métier, pas encore accepté par Kafka ; PUBLIE : acquitté par Kafka. */
public enum StatutOutbox {
    EN_ATTENTE,
    PUBLIE
}
