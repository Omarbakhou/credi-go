package com.credigo.credit.enums;

/** INACTIVE tant que les fonds ne sont pas versés ; A_PAYER une fois l'échéancier activé. */
public enum StatutEcheance {
    INACTIVE,
    A_PAYER,
    PAYEE,
    EN_RETARD
}
