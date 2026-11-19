package com.credigo.clients.dto;

import com.credigo.clients.enums.StatutClient;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;

/**
 * DTO d'entrée/sortie d'un client. L'id est ignoré en entrée (attribué par la base).
 * scoreInterne et statut ne sont pris en compte qu'à la création : ensuite ils évoluent
 * uniquement via les opérations métier prévues (activation, désactivation, scoring).
 */
public record ClientDto(
        Long id,

        @NotBlank(message = "Le nom est obligatoire")
        @Size(max = 100)
        String nom,

        @NotBlank(message = "L'email est obligatoire")
        @Email(message = "Format d'email invalide")
        String email,

        @NotNull(message = "Les revenus mensuels sont obligatoires")
        @PositiveOrZero(message = "Les revenus mensuels doivent être positifs")
        BigDecimal revenusMensuels,

        @Min(value = 0, message = "Le score doit être compris entre 0 et 1000")
        @Max(value = 1000, message = "Le score doit être compris entre 0 et 1000")
        Integer scoreInterne,

        StatutClient statut
) {
}
