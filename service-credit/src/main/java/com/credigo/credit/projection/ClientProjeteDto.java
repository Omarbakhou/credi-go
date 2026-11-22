package com.credigo.credit.projection;

import java.math.BigDecimal;

/** Vue d'un client lue dans la projection locale (mêmes champs que l'ancien DTO OpenFeign). */
public record ClientProjeteDto(
        Long id,
        String nom,
        String email,
        BigDecimal revenusMensuels,
        Integer scoreInterne,
        String statut
) {
}
