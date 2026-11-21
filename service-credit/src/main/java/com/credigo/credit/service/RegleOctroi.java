package com.credigo.credit.service;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Règles d'octroi simplifiées (volontairement lisibles, pas un vrai moteur de scoring) :
 * <ul>
 *   <li>taux selon le score : >= 750 -> 5,50 % ; >= 650 -> 6,50 % ; >= 550 -> 7,50 % ; sinon refus ;</li>
 *   <li>taux d'endettement : la 1ère mensualité ne dépasse pas 40 % des revenus mensuels ;</li>
 *   <li>accord partiel possible si la capacité couvre au moins 50 % du montant demandé ;</li>
 *   <li>au-delà de 100 000 MAD, les garanties doivent couvrir au moins 30 % du montant accordé.</li>
 * </ul>
 */
public final class RegleOctroi {

    public static final int SCORE_MINIMUM = 550;
    public static final BigDecimal TAUX_ENDETTEMENT_MAX = new BigDecimal("0.40");
    public static final BigDecimal SEUIL_GARANTIES = new BigDecimal("100000");
    public static final BigDecimal COUVERTURE_GARANTIES = new BigDecimal("0.30");

    private RegleOctroi() {
    }

    /** Taux annuel en pourcentage, ou null si le score est insuffisant. */
    public static BigDecimal tauxPourScore(int score) {
        if (score >= 750) return new BigDecimal("5.50");
        if (score >= 650) return new BigDecimal("6.50");
        if (score >= SCORE_MINIMUM) return new BigDecimal("7.50");
        return null;
    }

    /** Taux mensuel décimal à partir d'un taux annuel en pourcentage (6.50 -> 0.0054166...). */
    public static BigDecimal tauxMensuel(BigDecimal tauxAnnuelPourcent) {
        return tauxAnnuelPourcent.divide(new BigDecimal("1200"), 10, RoundingMode.HALF_UP);
    }

    /** Amortissement constant : 1ère mensualité = capital / n + intérêts du premier mois. */
    public static BigDecimal premiereMensualite(BigDecimal montant, int dureeMois, BigDecimal tauxAnnuel) {
        BigDecimal capital = montant.divide(BigDecimal.valueOf(dureeMois), 10, RoundingMode.HALF_UP);
        return capital.add(montant.multiply(tauxMensuel(tauxAnnuel))).setScale(2, RoundingMode.HALF_UP);
    }

    /** Montant maximal empruntable pour une mensualité donnée, arrondi au millier inférieur. */
    public static BigDecimal montantMaximal(BigDecimal mensualiteMax, int dureeMois, BigDecimal tauxAnnuel) {
        BigDecimal diviseur = BigDecimal.ONE.divide(BigDecimal.valueOf(dureeMois), 10, RoundingMode.HALF_UP)
                .add(tauxMensuel(tauxAnnuel));
        BigDecimal max = mensualiteMax.divide(diviseur, 0, RoundingMode.DOWN);
        return max.divide(new BigDecimal("1000"), 0, RoundingMode.DOWN).multiply(new BigDecimal("1000"))
                .setScale(2, RoundingMode.HALF_UP);
    }
}
