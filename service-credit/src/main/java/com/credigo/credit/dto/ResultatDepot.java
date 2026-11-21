package com.credigo.credit.dto;

/** Résultat d'un dépôt : nouveau dossier (201) ou rejeu idempotent d'une requête déjà traitée (200). */
public record ResultatDepot(LoanApplicationDto dossier, boolean nouveau) {
}
