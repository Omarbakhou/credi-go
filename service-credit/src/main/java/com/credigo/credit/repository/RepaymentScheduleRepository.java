package com.credigo.credit.repository;

import com.credigo.credit.entite.RepaymentSchedule;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RepaymentScheduleRepository extends JpaRepository<RepaymentSchedule, Long> {

    List<RepaymentSchedule> findByLoanApplicationIdOrderByNumeroEcheance(Long loanApplicationId);
}
