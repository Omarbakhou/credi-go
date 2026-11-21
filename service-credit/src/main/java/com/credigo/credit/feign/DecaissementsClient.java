package com.credigo.credit.feign;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@FeignClient(name = "service-decaissements")
public interface DecaissementsClient {

    @GetMapping("/decaissements")
    List<DecaissementDistantDto> getDecaissementsDuDossier(@RequestParam("dossierId") Long dossierId);
}
