package com.gestionstages.discovery;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.netflix.eureka.server.EnableEurekaServer;

/**
 * Service Registry de la plateforme.
 *
 * Tous les microservices s'enregistrent ici au demarrage. Le gateway
 * s'en sert pour resoudre les noms logiques (lb://auth-service) en
 * adresses reelles, sans qu'aucun service ne connaisse l'IP d'un autre.
 */
@EnableEurekaServer
@SpringBootApplication
public class DiscoveryServerApplication {

	public static void main(String[] args) {
		SpringApplication.run(DiscoveryServerApplication.class, args);
	}

}
