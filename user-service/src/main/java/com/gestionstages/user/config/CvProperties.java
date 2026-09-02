package com.gestionstages.user.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;

/** Stockage des CV etudiants, servi par config-repo/user-service.yml. */
@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "app.cv")
public class CvProperties {

    /** Repertoire de depot. Volume Docker en production. */
    private String location = "./cv";

    /**
     * PDF uniquement : c'est le format qu'attend une entreprise, et le
     * seul qui s'affiche a l'identique partout. Accepter du .docx
     * obligerait le destinataire a disposer du bon logiciel.
     */
    private List<String> allowedTypes = List.of("application/pdf");

    /** Un CV de plusieurs mega-octets est un CV mal exporte. */
    private long maxSizeBytes = 5 * 1024 * 1024;
}
