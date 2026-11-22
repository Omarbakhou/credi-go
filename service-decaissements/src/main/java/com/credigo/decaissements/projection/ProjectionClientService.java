package com.credigo.decaissements.projection;

import com.credigo.decaissements.messaging.ConsommateurIdempotent;
import com.credigo.decaissements.messaging.EvenementMetier;
import com.credigo.decaissements.messaging.Topics;
import com.credigo.decaissements.messaging.donnees.ClientDonnees;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Projection locale des clients (V7) : statut et coordonnées, utiles pour vérifier qu'un client est
 * actif avant de verser et pour adresser la notification. Application idempotente par version.
 */
@Service
@Transactional
public class ProjectionClientService {

    private final ClientProjectionRepository clientRepository;
    private final ConsommateurIdempotent lecteur;

    public ProjectionClientService(ClientProjectionRepository clientRepository, ConsommateurIdempotent lecteur) {
        this.clientRepository = clientRepository;
        this.lecteur = lecteur;
    }

    public String appliquer(EvenementMetier e) {
        ClientDonnees donnees = lecteur.donnees(e, ClientDonnees.class);
        Long clientId = donnees.id() != null ? donnees.id() : Long.valueOf(e.businessId());
        Optional<ClientProjection> existante = clientRepository.findById(clientId);
        if (existante.isPresent() && e.version() <= existante.get().getVersion()) {
            return "IGNORE : version " + e.version() + " déjà appliquée";
        }
        ClientProjection projection = existante.orElseGet(() -> new ClientProjection(clientId));
        projection.setVersion(e.version());
        projection.setDateMiseAJour(LocalDateTime.now());
        if (Topics.TYPE_CLIENT_DELETED.equals(e.eventType())) {
            projection.setSupprime(true);
        } else {
            projection.setSupprime(false);
            projection.setNom(donnees.nom());
            projection.setEmail(donnees.email());
            projection.setRevenusMensuels(donnees.revenusMensuels());
            projection.setScoreInterne(donnees.scoreInterne());
            projection.setStatut(donnees.statut());
        }
        clientRepository.save(projection);
        return "CLIENT " + clientId + (projection.isSupprime() ? " SUPPRIME" : " " + donnees.statut());
    }

    @Transactional(readOnly = true)
    public Optional<ClientProjection> client(Long clientId) {
        return clientRepository.findById(clientId).filter(c -> !c.isSupprime());
    }

    @Transactional(readOnly = true)
    public List<ClientProjeteDto> lister() {
        return clientRepository.findAll().stream().filter(c -> !c.isSupprime())
                .map(ProjectionClientService::toDto).toList();
    }

    public void vider() {
        clientRepository.deleteAllInBatch();
    }

    public static ClientProjeteDto toDto(ClientProjection c) {
        return new ClientProjeteDto(c.getClientId(), c.getNom(), c.getEmail(), c.getRevenusMensuels(),
                c.getScoreInterne(), c.getStatut());
    }
}
