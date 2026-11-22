package com.credigo.clients;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableDiscoveryClient
@EnableScheduling // publication de l'outbox (V7)
public class ServiceClientsApplication {

    public static void main(String[] args) {
        SpringApplication.run(ServiceClientsApplication.class, args);
    }
}
