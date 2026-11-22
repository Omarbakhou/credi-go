package com.credigo.credit.messaging;

import com.credigo.credit.messaging.outbox.OutboxEvent;
import com.credigo.credit.messaging.outbox.OutboxRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Supervision des événements (V7) :
 * GET /evenements/outbox[?correlationId=] : événements émis et leur statut de publication ;
 * GET /evenements/traites[?correlationId=] : événements consommés et la décision prise.
 */
@RestController
@RequestMapping("/evenements")
public class EvenementsController {

    private final OutboxRepository outboxRepository;
    private final EvenementTraiteRepository traiteRepository;
    private final ObjectMapper objectMapper;

    public EvenementsController(OutboxRepository outboxRepository, EvenementTraiteRepository traiteRepository,
                                ObjectMapper objectMapper) {
        this.outboxRepository = outboxRepository;
        this.traiteRepository = traiteRepository;
        this.objectMapper = objectMapper;
    }

    @GetMapping("/outbox")
    public List<OutboxDto> outbox(@RequestParam(required = false) String correlationId) {
        List<OutboxEvent> lignes = correlationId == null ? outboxRepository.findTop200ByOrderByIdDesc()
                : outboxRepository.findByCorrelationIdOrderByIdAsc(correlationId);
        return lignes.stream().map(this::toDto).toList();
    }

    @GetMapping("/traites")
    public List<EvenementTraite> traites(@RequestParam(required = false) String correlationId) {
        return correlationId == null ? traiteRepository.findTop200ByOrderByDateTraitementDesc()
                : traiteRepository.findByCorrelationIdOrderByDateTraitementAsc(correlationId);
    }

    private OutboxDto toDto(OutboxEvent o) {
        JsonNode payload;
        try {
            payload = objectMapper.readTree(o.getPayload());
        } catch (JsonProcessingException e) {
            payload = null;
        }
        return new OutboxDto(o.getId(), o.getEventId(), o.getEventType(), o.getTopic(), o.getCle(),
                o.getCorrelationId(), o.getStatut().name(), o.getTentatives(), o.getDerniereErreur(),
                o.getDateCreation(), o.getDatePublication(), payload);
    }

    public record OutboxDto(Long id, String eventId, String eventType, String topic, String cle,
                            String correlationId, String statut, int tentatives, String derniereErreur,
                            LocalDateTime dateCreation, LocalDateTime datePublication, JsonNode evenement) {
    }
}
