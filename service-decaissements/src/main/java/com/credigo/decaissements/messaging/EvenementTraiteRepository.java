package com.credigo.decaissements.messaging;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface EvenementTraiteRepository extends JpaRepository<EvenementTraite, String> {

    List<EvenementTraite> findTop200ByOrderByDateTraitementDesc();

    List<EvenementTraite> findByCorrelationIdOrderByDateTraitementAsc(String correlationId);
}
