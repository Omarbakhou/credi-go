package com.credigo.decaissements.feign;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "service-clients")
public interface ClientsClient {

    @GetMapping("/clients/{id}")
    ClientDistantDto getClient(@PathVariable("id") Long id);
}
