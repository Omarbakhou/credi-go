package com.credigo.credit.messaging.donnees;

/**
 * client-scored (service-clients -> service-credit) : score calculé pour une version précise de la
 * demande (versionDossier), ou refus si le client est inexistant ou inactif (clientEligible = false).
 */
public record ClientScoreDonnees(
        Long dossierId,
        Long clientId,
        long versionDossier,
        Integer score,
        boolean clientEligible,
        String motif
) {
}
