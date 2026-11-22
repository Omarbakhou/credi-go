package com.credigo.clients.service;

import com.credigo.clients.entite.Client;
import com.credigo.clients.enums.StatutClient;
import com.credigo.clients.messaging.ConsommateurIdempotent;
import com.credigo.clients.messaging.EvenementMetier;
import com.credigo.clients.messaging.Topics;
import com.credigo.clients.messaging.donnees.ClientScoreDonnees;
import com.credigo.clients.messaging.donnees.DemandeDeposeeDonnees;
import com.credigo.clients.messaging.outbox.OutboxService;
import com.credigo.clients.repository.ClientRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * application-deposited -> calcul du score du client pour cette demande ({@link RegleScoring}),
 * enregistré dans scoreInterne, puis client-scored (même correlationId, causationId = événement reçu).
 * Client inexistant ou inactif : client-scored avec clientEligible = false (refus du dossier).
 */
@Service
@Transactional
public class ScoringService {

    private final ClientRepository clientRepository;
    private final PublicationClients publicationClients;
    private final OutboxService outbox;
    private final ConsommateurIdempotent lecteur;

    public ScoringService(ClientRepository clientRepository, PublicationClients publicationClients,
                          OutboxService outbox, ConsommateurIdempotent lecteur) {
        this.clientRepository = clientRepository;
        this.publicationClients = publicationClients;
        this.outbox = outbox;
        this.lecteur = lecteur;
    }

    public String surDemandeDeposee(EvenementMetier e) {
        DemandeDeposeeDonnees demande = lecteur.donnees(e, DemandeDeposeeDonnees.class);
        Optional<Client> trouve = clientRepository.findById(demande.clientId());
        if (trouve.isEmpty()) {
            return publierScore(e, demande, null, false, "Client " + demande.clientId() + " inexistant");
        }
        Client client = trouve.get();
        if (client.getStatut() != StatutClient.ACTIF) {
            return publierScore(e, demande, null, false, "Client inactif");
        }
        int score = RegleScoring.calculer(client.getRevenusMensuels(), demande.montantDemande());
        client.setScoreInterne(score);
        publicationClients.publier(client, Topics.TYPE_CLIENT_UPDATED, e.correlationId(), e.eventId());
        return publierScore(e, demande, score, true, null);
    }

    private String publierScore(EvenementMetier recu, DemandeDeposeeDonnees demande, Integer score,
                                boolean eligible, String motif) {
        outbox.enregistrer(Topics.CLIENT_SCORED, Topics.TYPE_CLIENT_SCORED, String.valueOf(demande.dossierId()),
                demande.versionDossier(), recu.correlationId(), recu.eventId(),
                new ClientScoreDonnees(demande.dossierId(), demande.clientId(), demande.versionDossier(), score,
                        eligible, motif));
        return eligible ? "SCORE " + score + " (client " + demande.clientId() + ")" : "NON ELIGIBLE : " + motif;
    }
}
