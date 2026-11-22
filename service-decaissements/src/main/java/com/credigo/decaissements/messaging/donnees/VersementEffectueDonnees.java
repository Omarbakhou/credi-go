package com.credigo.decaissements.messaging.donnees;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** disbursement-paid (service-decaissements -> service-credit et notification-service). */
public record VersementEffectueDonnees(
        Long decaissementId,
        Long dossierId,
        Long clientId,
        String nomClient,
        String emailClient,
        Long treasuryAccountId,
        BigDecimal montant,
        LocalDateTime dateVersement
) {
}
