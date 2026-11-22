package com.credigo.credit.messaging.donnees;

import java.math.BigDecimal;

/**
 * loan-approved (service-credit -> service-decaissements et notification-service).
 * tentative > 1 : relance du décaissement après un échec (le client n'est pas notifié une seconde fois).
 */
public record CreditAccordeDonnees(
        Long dossierId,
        Long clientId,
        String nomClient,
        String emailClient,
        BigDecimal montantAccorde,
        int dureeMois,
        BigDecimal tauxPropose,
        int tentative
) {
}
