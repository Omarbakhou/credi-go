package com.credigo.credit.enums;

/**
 * Cycle de vie : DEPOSE -> EN_ETUDE -> ACCORDE -> DEBLOQUE, ou EN_ETUDE -> REFUSE.
 * ANNULE (V7) : demande annulée avant décision, ou délai d'étude dépassé ; un résultat tardif est ignoré.
 */
public enum StatutDossier {
    DEPOSE,
    EN_ETUDE,
    ACCORDE,
    DEBLOQUE,
    REFUSE,
    ANNULE
}
