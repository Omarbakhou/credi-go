package com.credigo.notification.service;

import com.credigo.notification.entite.Notification;
import com.credigo.notification.messaging.ConsommateurIdempotent;
import com.credigo.notification.messaging.EvenementIllisibleException;
import com.credigo.notification.messaging.EvenementMetier;
import com.credigo.notification.messaging.donnees.CreditAccordeDonnees;
import com.credigo.notification.messaging.donnees.VersementEffectueDonnees;
import com.credigo.notification.repository.NotificationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Seul service qui envoie des notifications (V7) : les emails sont construits ici à partir des
 * données des événements Kafka, puis envoyés au serveur SMTP de test MailHog (http://localhost:8025).
 * Un échec SMTP lève une exception : la transaction (journal + événement traité) est annulée et
 * Kafka redélivre le message (3 essais, puis topic .DLT).
 */
@Service
@Transactional
public class NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);

    private final JavaMailSender mailSender;
    private final NotificationRepository repository;
    private final ConsommateurIdempotent lecteur;

    public NotificationService(JavaMailSender mailSender, NotificationRepository repository,
                               ConsommateurIdempotent lecteur) {
        this.mailSender = mailSender;
        this.repository = repository;
        this.lecteur = lecteur;
    }

    /** loan-approved : notifier le client de l'accord (une seule fois, pas lors d'une relance du décaissement). */
    public String notifierAccord(EvenementMetier e) {
        CreditAccordeDonnees accord = lecteur.donnees(e, CreditAccordeDonnees.class);
        if (accord.tentative() > 1) {
            return "IGNORE : relance du décaissement (tentative " + accord.tentative() + "), accord déjà notifié";
        }
        String objet = "Credi-Go : votre crédit n° " + accord.dossierId() + " est accordé";
        String contenu = "Bonjour " + accord.nomClient() + ",\n\nVotre demande de crédit n° " + accord.dossierId()
                + " est accordée : " + accord.montantAccorde() + " MAD sur " + accord.dureeMois()
                + " mois au taux de " + accord.tauxPropose() + " %.\nLes fonds vous seront versés prochainement."
                + "\n\nCredi-Go";
        return envoyer(e, accord.dossierId(), accord.emailClient(), objet, contenu);
    }

    /** disbursement-paid : confirmer le versement des fonds. */
    public String notifierVersement(EvenementMetier e) {
        VersementEffectueDonnees v = lecteur.donnees(e, VersementEffectueDonnees.class);
        String objet = "Credi-Go : fonds versés pour votre crédit n° " + v.dossierId();
        String contenu = "Bonjour " + v.nomClient() + ",\n\nLe montant de " + v.montant() + " MAD de votre crédit n° "
                + v.dossierId() + " a été versé. Votre échéancier de remboursement est désormais actif.\n\nCredi-Go";
        return envoyer(e, v.dossierId(), v.emailClient(), objet, contenu);
    }

    @Transactional(readOnly = true)
    public List<Notification> journal() {
        return repository.findTop100ByOrderByIdDesc();
    }

    private String envoyer(EvenementMetier e, Long dossierId, String destinataire, String objet, String contenu) {
        if (destinataire == null || destinataire.isBlank()) {
            throw new EvenementIllisibleException("Événement " + e.eventId() + " sans destinataire", null);
        }
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom("noreply@credigo.ma");
        message.setTo(destinataire);
        message.setSubject(objet);
        message.setText(contenu);
        mailSender.send(message); // exception -> rollback -> nouvel essai par Kafka
        repository.save(new Notification(e.eventType(), dossierId, destinataire, objet, "ENVOYE", e.eventId(),
                e.correlationId()));
        log.info("Email envoyé à {} : {}", destinataire, objet);
        return "EMAIL ENVOYE à " + destinataire;
    }
}
