package com.credigo.credit.projection;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface GarantieProjectionRepository extends JpaRepository<GarantieProjection, Long> {

    List<GarantieProjection> findByClientIdOrderById(Long clientId);

    @Modifying(flushAutomatically = true)
    @Query("delete from GarantieProjection g where g.clientId = :clientId")
    void supprimerDuClient(Long clientId);
}
