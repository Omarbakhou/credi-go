package com.credigo.decaissements.messaging;

/** Topics Kafka et types d'événements de Credi-Go (V7). */
public final class Topics {

    // ----- Processus du crédit -----
    public static final String APPLICATION_DEPOSITED = "application-deposited";
    public static final String CLIENT_SCORED = "client-scored";
    public static final String LOAN_APPROVED = "loan-approved";
    public static final String DISBURSEMENT_PAID = "disbursement-paid";
    public static final String DISBURSEMENT_FAILED = "disbursement-failed";

    // ----- Données de référence (topic compacté : dernier état de chaque client) -----
    public static final String CLIENT_EVENTS = "client-events";

    // ----- Types d'événements -----
    public static final String TYPE_APPLICATION_DEPOSITED = "APPLICATION_DEPOSITED";
    public static final String TYPE_CLIENT_SCORED = "CLIENT_SCORED";
    public static final String TYPE_LOAN_APPROVED = "LOAN_APPROVED";
    public static final String TYPE_DISBURSEMENT_PAID = "DISBURSEMENT_PAID";
    public static final String TYPE_DISBURSEMENT_FAILED = "DISBURSEMENT_FAILED";
    public static final String TYPE_CLIENT_CREATED = "CLIENT_CREATED";
    public static final String TYPE_CLIENT_UPDATED = "CLIENT_UPDATED";
    public static final String TYPE_CLIENT_ACTIVATED = "CLIENT_ACTIVATED";
    public static final String TYPE_CLIENT_DEACTIVATED = "CLIENT_DEACTIVATED";
    public static final String TYPE_CLIENT_DELETED = "CLIENT_DELETED";
    /** Republication de l'état courant (initialisation ou reconstruction des projections). */
    public static final String TYPE_CLIENT_SNAPSHOT = "CLIENT_SNAPSHOT";

    private Topics() {
    }
}
