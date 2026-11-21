package com.credigo.credit.feign;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.List;

/** Client OpenFeign : le nom logique est résolu par Eureka (load balancing côté client). */
@FeignClient(name = "service-clients")
public interface ClientsClient {

    @GetMapping("/clients/{id}")
    ClientDistantDto getClient(@PathVariable("id") Long id);

    @GetMapping("/clients/{id}/garanties")
    List<GarantieDistanteDto> getGaranties(@PathVariable("id") Long id);
}
