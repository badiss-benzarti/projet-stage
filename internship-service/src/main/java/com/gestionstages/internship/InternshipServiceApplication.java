package com.gestionstages.internship;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.security.autoconfigure.UserDetailsServiceAutoConfiguration;
import org.springframework.cloud.openfeign.EnableFeignClients;

@EnableFeignClients
// Aucun utilisateur en memoire : toute identite vient du jeton JWT.
// Sans cette exclusion, Spring Boot genere un mot de passe au demarrage
// et pollue les logs avec un avertissement trompeur.
@SpringBootApplication(exclude = UserDetailsServiceAutoConfiguration.class)
public class InternshipServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(InternshipServiceApplication.class, args);
	}

}
