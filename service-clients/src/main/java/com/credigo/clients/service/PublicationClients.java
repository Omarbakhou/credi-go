package com.credigo.clients.service;

import com.credigo.clients.entite.Client;
import com.credigo.clients.messaging.Topics;
import com.credigo.clients.messaging.donnees.ClientDonnees;
import com.credigo.clients.messaging.outbox.OutboxService;
import com.credigo.clients.repository.ClientRepository;
import com.credigo.clients.repository.CollateralRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Publication des données de référence (V7) : chaque création, modification, (dés)activation,
 * suppression, changement de garanties ou nouveau score écrit dans l'outbox un instantané complet du
 * client sur le topic compacté client-events (clé = clientId). Les autres services en tirent leurs
 * projections locales ; service-clients reste le seul propriétaire et le seul à décider de ces données.
 */
@Component
@Transactional
public class PublicationClients {

    private final OutboxService outbox;
    private final ClientRepository clientRepository;
    private final CollateralRepository collateralRepository;

    public PublicationClients(OutboxService outbox, ClientRepository clientRepository,
                              CollateralRepository collateralRepository) {
        this.outbox = outbox;
        this.clientRepository = clientRepository;
        this.collateralRepository = collateralRepository;
    }

    public void publier(Client client, String eventType) {
        publier(client, eventType, UUID.randomUUID().toString(), null);
    }

    public void publier(Client client, String eventType, String correlationId, String causationId) {
        long version = client.incrementerVersion();
        outbox.enregistrer(Topics.CLIENT_EVENTS, eventType, String.valueOf(client.getId()), version,
                correlationId, causationId, instantane(client));
    }

    /** Initialisation / reconstruction des projections : republie l'état courant de tous les clients. */
    public int republierTous() {
        String correlationId = UUID.randomUUID().toString();
        var clients = clientRepository.findAll();
        clients.forEach(c -> publier(c, Topics.TYPE_CLIENT_SNAPSHOT, correlationId, null));
        return clients.size();
    }

    private ClientDonnees instantane(Client c) {
        var garanties = collateralRepository.findByClientId(c.getId()).stream()
                .map(g -> new ClientDonnees.GarantieDonnees(g.getId(), g.getType().name(), g.getValeur(),
                        g.getDescription()))
                .toList();
        return new ClientDonnees(c.getId(), c.getNom(), c.getEmail(), c.getRevenusMensuels(), c.getScoreInterne(),
                c.getStatut().name(), garanties);
    }
}
