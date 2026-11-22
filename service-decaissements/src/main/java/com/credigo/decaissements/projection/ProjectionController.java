package com.credigo.decaissements.projection;

import com.credigo.decaissements.exception.RessourceIntrouvableException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/** Consultation et reconstruction de la projection locale des clients (V7). */
@RestController
@RequestMapping("/projections/clients")
public class ProjectionController {

    private final ProjectionClientService projectionService;
    private final ProjectionClientsListener listener;

    public ProjectionController(ProjectionClientService projectionService, ProjectionClientsListener listener) {
        this.projectionService = projectionService;
        this.listener = listener;
    }

    @GetMapping
    public List<ClientProjeteDto> lister() {
        return projectionService.lister();
    }

    @GetMapping("/{id}")
    public ClientProjeteDto consulter(@PathVariable Long id) {
        return projectionService.client(id).map(ProjectionClientService::toDto)
                .orElseThrow(() -> new RessourceIntrouvableException("Client " + id + " absent de la projection locale"));
    }

    /** 202 : la reconstruction est asynchrone (relecture du topic par le consommateur Kafka). */
    @PostMapping("/reconstruction")
    public ResponseEntity<Map<String, String>> reconstruire() {
        listener.reconstruire();
        return ResponseEntity.accepted().body(Map.of("message",
                "Projection vidée : relecture du topic client-events depuis le début en cours"));
    }
}
