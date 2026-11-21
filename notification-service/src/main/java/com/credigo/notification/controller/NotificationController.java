package com.credigo.notification.controller;

import com.credigo.notification.service.NotificationService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Confirmation de réception : dernières notifications consommées et leur statut d'envoi. */
@RestController
@RequestMapping("/notifications")
public class NotificationController {

    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @GetMapping
    public List<NotificationService.NotificationEnvoyee> lister() {
        return notificationService.journal();
    }
}
