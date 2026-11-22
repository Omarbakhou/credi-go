package com.credigo.clients.service;

import java.math.BigDecimal;

/**
 * Règle de scoring simplifiée (V7), volontairement lisible : le score dépend des revenus mensuels,
 * avec une pénalité si la demande est très élevée au regard des revenus.
 * <ul>
 *   <li>revenus >= 15 000 MAD -> 800 ; >= 10 000 -> 720 ; >= 8 000 -> 650 ; >= 5 000 -> 560 ; sinon 480 ;</li>
 *   <li>montant demandé > 30 fois les revenus mensuels -> score - 50 (minimum 300).</li>
 * </ul>
 * Rappel des seuils d'octroi (service-credit) : 750 -> 5,50 % ; 650 -> 6,50 % ; 550 -> 7,50 % ; dessous : refus.
 */
public final class RegleScoring {

    private static final BigDecimal MULTIPLE_REVENUS_MAX = new BigDecimal("30");

    private RegleScoring() {
    }

    public static int calculer(BigDecimal revenusMensuels, BigDecimal montantDemande) {
        int score = scoreRevenus(revenusMensuels);
        if (montantDemande != null && montantDemande.compareTo(revenusMensuels.multiply(MULTIPLE_REVENUS_MAX)) > 0) {
            score -= 50;
        }
        return Math.max(300, score);
    }

    private static int scoreRevenus(BigDecimal revenus) {
        if (revenus.compareTo(new BigDecimal("15000")) >= 0) return 800;
        if (revenus.compareTo(new BigDecimal("10000")) >= 0) return 720;
        if (revenus.compareTo(new BigDecimal("8000")) >= 0) return 650;
        if (revenus.compareTo(new BigDecimal("5000")) >= 0) return 560;
        return 480;
    }
}
