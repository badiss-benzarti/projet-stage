package com.gestionstages.document.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;

/** Parametres de stockage, servis par config-repo/document-service.yml. */
@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "app.storage")
public class StorageProperties {

    /** Repertoire de depot. Volume Docker en production. */
    private String location = "./storage";

    /** Types MIME acceptes. Tout le reste est refuse a l'entree. */
    private List<String> allowedTypes = List.of("application/pdf", "image/png", "image/jpeg");
}
