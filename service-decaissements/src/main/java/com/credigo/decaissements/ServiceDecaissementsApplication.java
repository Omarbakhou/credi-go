package com.credigo.decaissements;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableDiscoveryClient
@EnableScheduling // publication de l'outbox et expiration des réservations (V7)
public class ServiceDecaissementsApplication {

    public static void main(String[] args) {
        SpringApplication.run(ServiceDecaissementsApplication.class, args);
    }
}
