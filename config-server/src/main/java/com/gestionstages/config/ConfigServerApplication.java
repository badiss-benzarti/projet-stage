package com.gestionstages.config;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.config.server.EnableConfigServer;

/**
 * Configuration centralisee de la plateforme.
 *
 * Sert les fichiers du dossier config-repo/ a tous les microservices :
 *   application.yml            -> commun a tous
 *   <spring.application.name>.yml -> specifique a un service
 *
 * Mode "native" : les configurations sont lues depuis le systeme de
 * fichiers, pas depuis un depot Git distant. Le config-repo fait partie
 * du projet, il n'y a donc rien a cloner.
 */
@EnableConfigServer
@SpringBootApplication
public class ConfigServerApplication {

	public static void main(String[] args) {
		SpringApplication.run(ConfigServerApplication.class, args);
	}

}
