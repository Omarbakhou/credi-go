package com.credigo.credit;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@SpringBootApplication
@EnableDiscoveryClient
public class ServiceCreditApplication {

    public static void main(String[] args) {
        SpringApplication.run(ServiceCreditApplication.class, args);
    }
}
