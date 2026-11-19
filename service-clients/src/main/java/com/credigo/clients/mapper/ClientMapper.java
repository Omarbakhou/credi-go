package com.credigo.clients.mapper;

import com.credigo.clients.dto.ClientDto;
import com.credigo.clients.dto.CollateralDto;
import com.credigo.clients.entite.Client;
import com.credigo.clients.entite.Collateral;
import com.credigo.clients.enums.StatutClient;
import org.springframework.stereotype.Component;

/**
 * Conversion Entité <-> DTO. L'API n'expose jamais directement les entités JPA.
 */
@Component
public class ClientMapper {

    public ClientDto toDto(Client client) {
        return new ClientDto(client.getId(), client.getNom(), client.getEmail(),
                client.getRevenusMensuels(), client.getScoreInterne(), client.getStatut());
    }

    /** Création : statut ACTIF et score 0 par défaut. */
    public Client toNouvelleEntite(ClientDto dto) {
        Client client = new Client();
        copierCoordonnees(dto, client);
        client.setScoreInterne(dto.scoreInterne() == null ? 0 : dto.scoreInterne());
        client.setStatut(dto.statut() == null ? StatutClient.ACTIF : dto.statut());
        return client;
    }

    /** Mise à jour : seules les coordonnées et les revenus sont modifiables librement. */
    public void copierCoordonnees(ClientDto dto, Client client) {
        client.setNom(dto.nom());
        client.setEmail(dto.email().toLowerCase());
        client.setRevenusMensuels(dto.revenusMensuels());
    }

    public CollateralDto toDto(Collateral garantie) {
        return new CollateralDto(garantie.getId(), garantie.getClient().getId(), garantie.getType(),
                garantie.getValeur(), garantie.getDescription());
    }

    public void copier(CollateralDto dto, Collateral garantie, Client client) {
        garantie.setClient(client);
        garantie.setType(dto.type());
        garantie.setValeur(dto.valeur());
        garantie.setDescription(dto.description());
    }
}
