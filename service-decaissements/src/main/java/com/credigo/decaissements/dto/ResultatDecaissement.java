package com.credigo.decaissements.dto;

/** Nouveau versement (201) ou rejeu idempotent d'une requête déjà traitée (200). */
public record ResultatDecaissement(DisbursementDto decaissement, boolean nouveau) {
}
