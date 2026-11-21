package com.credigo.decaissements.repository;

import com.credigo.decaissements.entite.Disbursement;
import org.springframework.data.jpa.repository.JpaRepository;

import com.credigo.decaissements.enums.StatutDecaissement;

import java.util.List;
import java.util.Optional;

public interface DisbursementRepository extends JpaRepository<Disbursement, Long> {

    boolean existsByTreasuryAccountId(Long treasuryAccountId);

    List<Disbursement> findByLoanApplicationId(Long loanApplicationId);

    boolean existsByLoanApplicationIdAndStatut(Long loanApplicationId, StatutDecaissement statut);

    Optional<Disbursement> findByIdempotencyKey(String idempotencyKey);
}
