package com.credigo.decaissements.messaging.donnees;

import java.math.BigDecimal;

/**
 * disbursement-failed (service-decaissements -> service-credit) : le versement n'a pas eu lieu,
 * les fonds éventuellement réservés ont été libérés (fondsLiberes).
 */
public record VersementEchoueDonnees(
        Long decaissementId,
        Long dossierId,
        Long treasuryAccountId,
        BigDecimal montant,
        String statut,
        String motif,
        BigDecimal fondsLiberes
) {
}
