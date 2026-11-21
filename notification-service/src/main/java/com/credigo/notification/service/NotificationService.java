package com.credigo.notification.service;

import com.credigo.notification.messaging.NotificationEmailDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Envoi des emails (serveur SMTP de test MailHog, interface web http://localhost:8025)
 * et journal en mémoire des notifications traitées, consultable via GET /notifications.
 */
@Service
public class NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);
    private static final int TAILLE_JOURNAL = 100;

    private final JavaMailSender mailSender;
    private final List<NotificationEnvoyee> journal = Collections.synchronizedList(new ArrayList<>());

    public NotificationService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    public void envoyerEmail(NotificationEmailDto notification) {
        String statut;
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom("noreply@credigo.ma");
            message.setTo(notification.destinataire());
            message.setSubject(notification.objet());
            message.setText(notification.contenu());
            mailSender.send(message);
            statut = "ENVOYE";
            log.info("Email envoyé à {} : {}", notification.destinataire(), notification.objet());
        } catch (Exception e) {
            statut = "ECHEC";
            log.error("Échec d'envoi de l'email à {} : {}", notification.destinataire(), e.getMessage());
        }
        journal.add(0, new NotificationEnvoyee(LocalDateTime.now(), notification.evenement(),
                notification.dossierId(), notification.destinataire(), notification.objet(), statut));
        if (journal.size() > TAILLE_JOURNAL) {
            journal.remove(journal.size() - 1);
        }
    }

    public List<NotificationEnvoyee> journal() {
        synchronized (journal) {
            return List.copyOf(journal);
        }
    }

    public record NotificationEnvoyee(LocalDateTime date, String evenement, Long dossierId,
                                      String destinataire, String objet, String statut) {
    }
}
