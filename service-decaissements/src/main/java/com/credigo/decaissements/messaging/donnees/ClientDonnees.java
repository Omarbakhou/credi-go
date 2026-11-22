package com.credigo.decaissements.messaging.donnees;

import java.math.BigDecimal;
import java.util.List;

/** Instantané d'un client publié sur client-events (création, modification, (dés)activation, scoring). */
public record ClientDonnees(
        Long id,
        String nom,
        String email,
        BigDecimal revenusMensuels,
        Integer scoreInterne,
        String statut,
        List<GarantieDonnees> garanties
) {
    public record GarantieDonnees(Long id, String type, BigDecimal valeur, String description) {
    }
}
