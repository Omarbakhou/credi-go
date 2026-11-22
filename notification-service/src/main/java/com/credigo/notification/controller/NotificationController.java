package com.credigo.notification.controller;

import com.credigo.notification.entite.Notification;
import com.credigo.notification.messaging.EvenementTraite;
import com.credigo.notification.messaging.EvenementTraiteRepository;
import com.credigo.notification.service.NotificationService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * GET /notifications : dernières notifications envoyées (confirmation de réception) ;
 * GET /evenements/traites : événements Kafka consommés et décision prise (V7).
 */
@RestController
public class NotificationController {

    private final NotificationService notificationService;
    private final EvenementTraiteRepository traiteRepository;

    public NotificationController(NotificationService notificationService, EvenementTraiteRepository traiteRepository) {
        this.notificationService = notificationService;
        this.traiteRepository = traiteRepository;
    }

    @GetMapping("/notifications")
    public List<Notification> lister() {
        return notificationService.journal();
    }

    @GetMapping("/evenements/traites")
    public List<EvenementTraite> traites(@RequestParam(required = false) String correlationId) {
        return correlationId == null ? traiteRepository.findTop200ByOrderByDateTraitementDesc()
                : traiteRepository.findByCorrelationIdOrderByDateTraitementAsc(correlationId);
    }
}
