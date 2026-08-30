package com.gestionstages.user.service;

import com.gestionstages.user.config.PhotoProperties;
import com.gestionstages.user.exception.ApiExceptions;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.*;
import java.util.UUID;

/**
 * Photos de profil sur disque.
 *
 * Le nom d'origine n'est jamais reutilise : un fichier nomme
 * "../../application.yml" permettrait d'ecrire hors du repertoire.
 * On genere un UUID et on ne conserve que le type MIME en base.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PhotoStorageService {

    private final PhotoProperties props;
    private Path racine;

    @PostConstruct
    void init() {
        this.racine = Paths.get(props.getLocation()).toAbsolutePath().normalize();
        try {
            Files.createDirectories(racine);
            log.info("Repertoire des photos : {}", racine);
        } catch (IOException e) {
            throw new IllegalStateException("Repertoire des photos inaccessible : " + racine, e);
        }
    }

    /** @return le nom sous lequel la photo a ete enregistree. */
    public String store(MultipartFile fichier) {
        if (fichier == null || fichier.isEmpty()) {
            throw new ApiExceptions.BusinessRuleException("La photo est vide");
        }
        if (!props.getAllowedTypes().contains(fichier.getContentType())) {
            throw new ApiExceptions.BusinessRuleException(
                    "Format refuse : " + fichier.getContentType()
                            + ". Acceptes : " + String.join(", ", props.getAllowedTypes()));
        }
        if (fichier.getSize() > props.getMaxSizeBytes()) {
            throw new ApiExceptions.BusinessRuleException(
                    "Photo trop lourde : " + (props.getMaxSizeBytes() / 1024 / 1024) + " Mo maximum");
        }

        String nom = UUID.randomUUID() + extension(fichier.getContentType());
        Path cible = racine.resolve(nom).normalize();

        if (!cible.startsWith(racine)) {
            throw new ApiExceptions.BusinessRuleException("Chemin de destination invalide");
        }

        try (var in = fichier.getInputStream()) {
            Files.copy(in, cible, StandardCopyOption.REPLACE_EXISTING);
            return nom;
        } catch (IOException e) {
            throw new IllegalStateException("Ecriture de la photo impossible", e);
        }
    }

    public byte[] read(String nom) {
        Path fichier = racine.resolve(nom).normalize();
        if (!fichier.startsWith(racine) || !Files.exists(fichier)) {
            throw new ApiExceptions.NotFoundException("Photo", nom);
        }
        try {
            return Files.readAllBytes(fichier);
        } catch (IOException e) {
            throw new IllegalStateException("Lecture de la photo impossible", e);
        }
    }

    public void delete(String nom) {
        if (nom == null) {
            return;
        }
        try {
            Files.deleteIfExists(racine.resolve(nom).normalize());
        } catch (IOException e) {
            log.warn("Suppression de {} impossible : {}", nom, e.getMessage());
        }
    }

    private String extension(String typeMime) {
        return switch (typeMime == null ? "" : typeMime) {
            case "image/png" -> ".png";
            case "image/webp" -> ".webp";
            default -> ".jpg";
        };
    }
}
