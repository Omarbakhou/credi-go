package com.credigo.decaissements.feign;

import com.credigo.decaissements.exception.RegleMetierException;
import com.credigo.decaissements.exception.RessourceIntrouvableException;
import com.credigo.decaissements.exception.ServiceExterneIndisponibleException;
import feign.FeignException;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import org.springframework.stereotype.Component;

/**
 * Appels OpenFeign protégés par Resilience4j (Retry + Circuit Breaker + timeouts OpenFeign).
 * Politique stricte : sans réponse fiable du service distant, le décaissement est refusé (503).
 */
@Component
public class ResilientRequests {

    private final CreditClient creditClient;
    private final ClientsClient clientsClient;

    public ResilientRequests(CreditClient creditClient, ClientsClient clientsClient) {
        this.creditClient = creditClient;
        this.clientsClient = clientsClient;
    }

    @Retry(name = "service-credit", fallbackMethod = "dossierIndisponible")
    @CircuitBreaker(name = "service-credit")
    public DossierDistantDto dossier(Long dossierId) {
        return creditClient.getDossier(dossierId);
    }

    @Retry(name = "service-credit", fallbackMethod = "deblocageIndisponible")
    @CircuitBreaker(name = "service-credit")
    public DossierDistantDto debloquer(Long dossierId) {
        return creditClient.debloquer(dossierId);
    }

    @Retry(name = "service-clients", fallbackMethod = "clientIndisponible")
    @CircuitBreaker(name = "service-clients")
    public ClientDistantDto client(Long clientId) {
        return clientsClient.getClient(clientId);
    }

    // ===================== Fallbacks =====================

    private DossierDistantDto dossierIndisponible(Long dossierId, Throwable ex) {
        traduireErreurMetier(ex, "Dossier de crédit " + dossierId + " introuvable dans service-credit");
        throw new ServiceExterneIndisponibleException(
                "service-credit est momentanément indisponible. Réessayez dans quelques instants.");
    }

    private DossierDistantDto deblocageIndisponible(Long dossierId, Throwable ex) {
        traduireErreurMetier(ex, "Dossier de crédit " + dossierId + " introuvable dans service-credit");
        throw new ServiceExterneIndisponibleException(
                "service-credit n'a pas confirmé le déblocage : versement annulé, fonds libérés.");
    }

    private ClientDistantDto clientIndisponible(Long clientId, Throwable ex) {
        traduireErreurMetier(ex, "Client " + clientId + " introuvable dans service-clients");
        throw new ServiceExterneIndisponibleException(
                "service-clients est momentanément indisponible. Réessayez dans quelques instants.");
    }

    /** 404 -> ressource introuvable ; 409 distant -> règle métier ; autres 4xx relancées. */
    private void traduireErreurMetier(Throwable ex, String messageIntrouvable) {
        if (ex instanceof FeignException.NotFound) {
            throw new RessourceIntrouvableException(messageIntrouvable);
        }
        if (ex instanceof FeignException.Conflict) {
            throw new RegleMetierException("Refus du service distant : " + ex.getMessage());
        }
        if (ex instanceof FeignException feign && feign.status() >= 400 && feign.status() < 500) {
            throw feign;
        }
    }
}
