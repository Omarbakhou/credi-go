package com.credigo.credit.repository;

import com.credigo.credit.entite.LoanApplication;
import com.credigo.credit.enums.StatutDossier;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface LoanApplicationRepository extends JpaRepository<LoanApplication, Long> {

    List<LoanApplication> findByClientId(Long clientId);

    List<LoanApplication> findByStatut(StatutDossier statut);

    Optional<LoanApplication> findByIdempotencyKey(String idempotencyKey);
}
