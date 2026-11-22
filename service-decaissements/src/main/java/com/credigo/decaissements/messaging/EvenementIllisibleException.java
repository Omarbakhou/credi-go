package com.credigo.decaissements.messaging;

/** Message Kafka impossible à interpréter : non rejoué, envoyé directement dans le topic .DLT. */
public class EvenementIllisibleException extends RuntimeException {

    public EvenementIllisibleException(String message, Throwable cause) {
        super(message, cause);
    }
}
