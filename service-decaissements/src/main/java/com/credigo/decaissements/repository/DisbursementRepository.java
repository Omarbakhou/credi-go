package com.credigo.decaissements.repository;

import com.credigo.decaissements.entite.Disbursement;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DisbursementRepository extends JpaRepository<Disbursement, Long> {

    boolean existsByTreasuryAccountId(Long treasuryAccountId);

    List<Disbursement> findByLoanApplicationId(Long loanApplicationId);
}
