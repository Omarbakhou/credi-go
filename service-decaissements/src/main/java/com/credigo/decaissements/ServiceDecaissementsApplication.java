package com.credigo.decaissements;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication
@EnableDiscoveryClient
@EnableFeignClients
public class ServiceDecaissementsApplication {

    public static void main(String[] args) {
        SpringApplication.run(ServiceDecaissementsApplication.class, args);
    }
}
