package com.credigo.decaissements.dto;

import jakarta.validation.constraints.NotNull;

/** Demande de versement : le montant n'est pas fourni, c'est le montantAccorde du dossier. */
public record DemandeDecaissementDto(
        @NotNull(message = "L'identifiant du dossier de crédit est obligatoire")
        Long loanApplicationId,

        @NotNull(message = "L'identifiant du compte de trésorerie est obligatoire")
        Long treasuryAccountId
) {
}
