package com.credigo.decaissements.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

/**
 * soldeDisponible n'est pris en compte qu'à la création (solde initial, 0 par défaut) :
 * ensuite il évolue uniquement par approvisionnement, réservation ou libération de fonds.
 * montantReserve (lecture seule, V7) : fonds réservés pour des décaissements pas encore versés.
 */
public record TreasuryAccountDto(
        Long id,

        @NotBlank(message = "Le libellé est obligatoire")
        @Size(max = 100)
        String libelle,

        @PositiveOrZero(message = "Le solde initial doit être positif ou nul")
        BigDecimal soldeDisponible,

        BigDecimal montantReserve
) {
}
