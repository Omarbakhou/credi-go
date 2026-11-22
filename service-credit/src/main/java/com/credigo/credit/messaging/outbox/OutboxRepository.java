package com.credigo.credit.messaging.outbox;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OutboxRepository extends JpaRepository<OutboxEvent, Long> {

    List<OutboxEvent> findTop50ByStatutOrderByIdAsc(StatutOutbox statut);

    List<OutboxEvent> findTop200ByOrderByIdDesc();

    List<OutboxEvent> findByCorrelationIdOrderByIdAsc(String correlationId);
}
