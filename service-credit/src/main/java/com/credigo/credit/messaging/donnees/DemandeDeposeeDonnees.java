package com.credigo.credit.messaging.donnees;

import java.math.BigDecimal;

/** application-deposited (service-credit -> service-clients) : demande à scorer. */
public record DemandeDeposeeDonnees(
        Long dossierId,
        Long clientId,
        BigDecimal montantDemande,
        int dureeMois,
        long versionDossier
) {
}
