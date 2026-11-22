package com.credigo.credit.projection;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DecaissementProjectionRepository extends JpaRepository<DecaissementProjection, Long> {

    List<DecaissementProjection> findByDossierIdOrderByDecaissementId(Long dossierId);
}
