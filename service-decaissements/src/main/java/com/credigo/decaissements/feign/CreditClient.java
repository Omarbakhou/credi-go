package com.credigo.decaissements.feign;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;

@FeignClient(name = "service-credit")
public interface CreditClient {

    @GetMapping("/credits/{id}")
    DossierDistantDto getDossier(@PathVariable("id") Long id);

    /** Passe le dossier à DEBLOQUE et active l'échéancier (idempotent côté service-credit). */
    @PostMapping("/credits/{id}/deblocage")
    DossierDistantDto debloquer(@PathVariable("id") Long id);
}
