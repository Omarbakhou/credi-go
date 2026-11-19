package com.credigo.credit.enums;

/** Cycle de vie : DEPOSE -> EN_ETUDE -> ACCORDE -> DEBLOQUE, ou EN_ETUDE -> REFUSE. */
public enum StatutDossier {
    DEPOSE,
    EN_ETUDE,
    ACCORDE,
    DEBLOQUE,
    REFUSE
}
