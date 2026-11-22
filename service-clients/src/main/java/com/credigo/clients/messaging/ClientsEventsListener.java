package com.credigo.clients.messaging;

import com.credigo.clients.service.PublicationClients;
import com.credigo.clients.service.ScoringService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.http.ResponseEntity;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Événements de service-clients (V7) :
 * <ul>
 *   <li>consomme application-deposited (groupe service-clients) et calcule le score ;</li>
 *   <li>au démarrage, republie l'état de tous les clients sur client-events (initialisation des
 *   projections : les clients de data.sql n'ont jamais été publiés) ;</li>
 *   <li>POST /evenements/clients/republication : même republication à la demande.</li>
 * </ul>
 */
@RestController
public class ClientsEventsListener {

    private static final Logger log = LoggerFactory.getLogger(ClientsEventsListener.class);

    private final ConsommateurIdempotent consommateur;
    private final ScoringService scoringService;
    private final PublicationClients publicationClients;

    public ClientsEventsListener(ConsommateurIdempotent consommateur, ScoringService scoringService,
                                 PublicationClients publicationClients) {
        this.consommateur = consommateur;
        this.scoringService = scoringService;
        this.publicationClients = publicationClients;
    }

    @KafkaListener(topics = Topics.APPLICATION_DEPOSITED)
    public void surDemandeDeposee(String message) {
        consommateur.traiter(Topics.APPLICATION_DEPOSITED, message, scoringService::surDemandeDeposee);
    }

    @EventListener(ApplicationReadyEvent.class)
    public void initialiserProjections() {
        int n = publicationClients.republierTous();
        log.info("Initialisation des projections : {} client(s) republié(s) sur {}", n, Topics.CLIENT_EVENTS);
    }

    @PostMapping("/evenements/clients/republication")
    public ResponseEntity<Map<String, Object>> republier() {
        int n = publicationClients.republierTous();
        return ResponseEntity.accepted().body(Map.of("clientsRepublies", n, "topic", Topics.CLIENT_EVENTS));
    }
}
