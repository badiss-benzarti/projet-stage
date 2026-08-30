package com.gestionstages.user.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;

/** Stockage des photos de profil, servi par config-repo/user-service.yml. */
@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "app.photos")
public class PhotoProperties {

    /** Repertoire de depot. Volume Docker en production. */
    private String location = "./photos";

    private List<String> allowedTypes = List.of("image/jpeg", "image/png", "image/webp");

    /** Une photo de profil n'a aucune raison de peser davantage. */
    private long maxSizeBytes = 2 * 1024 * 1024;
}
