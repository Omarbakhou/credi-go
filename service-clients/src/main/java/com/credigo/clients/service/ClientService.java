package com.credigo.clients.service;

import com.credigo.clients.dto.ClientDto;
import com.credigo.clients.entite.Client;
import com.credigo.clients.enums.StatutClient;
import com.credigo.clients.exception.RegleMetierException;
import com.credigo.clients.exception.RessourceIntrouvableException;
import com.credigo.clients.mapper.ClientMapper;
import com.credigo.clients.messaging.Topics;
import com.credigo.clients.repository.ClientRepository;
import com.credigo.clients.repository.CollateralRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class ClientService {

    private final ClientRepository clientRepository;
    private final CollateralRepository collateralRepository;
    private final ClientMapper mapper;
    private final PublicationClients publication;

    public ClientService(ClientRepository clientRepository, CollateralRepository collateralRepository,
                         ClientMapper mapper, PublicationClients publication) {
        this.clientRepository = clientRepository;
        this.collateralRepository = collateralRepository;
        this.mapper = mapper;
        this.publication = publication;
    }

    @Transactional(readOnly = true)
    public List<ClientDto> lister() {
        return clientRepository.findAll().stream().map(mapper::toDto).toList();
    }

    @Transactional(readOnly = true)
    public ClientDto consulter(Long id) {
        return mapper.toDto(trouver(id));
    }

    public ClientDto creer(ClientDto dto) {
        if (clientRepository.existsByEmail(dto.email().toLowerCase())) {
            throw new RegleMetierException("Un client existe déjà avec l'email " + dto.email());
        }
        Client client = clientRepository.save(mapper.toNouvelleEntite(dto));
        publication.publier(client, Topics.TYPE_CLIENT_CREATED); // V7 : projections des autres services
        return mapper.toDto(client);
    }

    public ClientDto modifier(Long id, ClientDto dto) {
        Client client = trouver(id);
        if (clientRepository.existsByEmailAndIdNot(dto.email().toLowerCase(), id)) {
            throw new RegleMetierException("L'email " + dto.email() + " est déjà utilisé par un autre client");
        }
        mapper.copierCoordonnees(dto, client);
        publication.publier(client, Topics.TYPE_CLIENT_UPDATED);
        return mapper.toDto(client);
    }

    /** Changement de statut par opération dédiée (et non par modification libre du champ). */
    public ClientDto changerStatut(Long id, StatutClient nouveauStatut) {
        Client client = trouver(id);
        if (client.getStatut() == nouveauStatut) {
            throw new RegleMetierException("Le client " + id + " est déjà " + nouveauStatut);
        }
        client.setStatut(nouveauStatut);
        publication.publier(client, nouveauStatut == StatutClient.ACTIF ? Topics.TYPE_CLIENT_ACTIVATED
                : Topics.TYPE_CLIENT_DEACTIVATED);
        return mapper.toDto(client);
    }

    /** Suppression refusée si le client possède encore des garanties (dépendances). */
    public void supprimer(Long id) {
        Client client = trouver(id);
        if (collateralRepository.existsByClientId(id)) {
            throw new RegleMetierException("Suppression impossible : le client " + id
                    + " possède des garanties. Supprimez-les ou passez le client INACTIF.");
        }
        publication.publier(client, Topics.TYPE_CLIENT_DELETED);
        clientRepository.delete(client);
    }

    Client trouver(Long id) {
        return clientRepository.findById(id)
                .orElseThrow(() -> new RessourceIntrouvableException("Client " + id + " introuvable"));
    }
}
