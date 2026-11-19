package com.credigo.clients.repository;

import com.credigo.clients.entite.Collateral;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CollateralRepository extends JpaRepository<Collateral, Long> {

    List<Collateral> findByClientId(Long clientId);

    boolean existsByClientId(Long clientId);
}
