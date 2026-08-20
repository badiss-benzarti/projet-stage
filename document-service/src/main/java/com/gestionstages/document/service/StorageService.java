package com.gestionstages.document.service;

import com.gestionstages.document.config.StorageProperties;
import com.gestionstages.document.exception.ApiExceptions;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.*;
import java.util.UUID;

/**
 * Ecriture et lecture des fichiers sur disque.
 *
 * Le nom d'origine n'est JAMAIS utilise comme nom de fichier : un nom
 * comme "../../application.yml" permettrait d'ecrire hors du repertoire
 * de stockage. On genere un UUID et on conserve le nom d'origine
 * uniquement comme metadonnee d'affichage.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class StorageService {

    private final StorageProperties props;
    private Path racine;

    @PostConstruct
    void init() {
        this.racine = Paths.get(props.getLocation()).toAbsolutePath().normalize();
        try {
            Files.createDirectories(racine);
            log.info("Repertoire de stockage : {}", racine);
        } catch (IOException e) {
            throw new IllegalStateException("Repertoire de stockage inaccessible : " + racine, e);
        }
    }

    /** @return le nom sous lequel le fichier a ete enregistre. */
    public String store(MultipartFile fichier, String extensionSouhaitee) {
        if (fichier.isEmpty()) {
            throw new ApiExceptions.BusinessRuleException("Le fichier est vide");
        }
        if (!props.getAllowedTypes().contains(fichier.getContentType())) {
            throw new ApiExceptions.BusinessRuleException(
                    "Type de fichier refuse : " + fichier.getContentType()
                            + ". Acceptes : " + String.join(", ", props.getAllowedTypes()));
        }

        String nom = UUID.randomUUID() + extension(fichier.getOriginalFilename(), extensionSouhaitee);
        Path cible = racine.resolve(nom).normalize();

        if (!cible.startsWith(racine)) {
            throw new ApiExceptions.BusinessRuleException("Chemin de destination invalide");
        }

        try (var in = fichier.getInputStream()) {
            Files.copy(in, cible, StandardCopyOption.REPLACE_EXISTING);
            return nom;
        } catch (IOException e) {
            throw new IllegalStateException("Ecriture du fichier impossible", e);
        }
    }

    /** Enregistre un contenu deja produit en memoire (attestation generee). */
    public String storeBytes(byte[] contenu, String extension) {
        String nom = UUID.randomUUID() + extension;
        try {
            Files.write(racine.resolve(nom), contenu);
            return nom;
        } catch (IOException e) {
            throw new IllegalStateException("Ecriture du fichier impossible", e);
        }
    }

    public byte[] read(String storedName) {
        Path fichier = racine.resolve(storedName).normalize();
        if (!fichier.startsWith(racine) || !Files.exists(fichier)) {
            throw new ApiExceptions.NotFoundException("Fichier", storedName);
        }
        try {
            return Files.readAllBytes(fichier);
        } catch (IOException e) {
            throw new IllegalStateException("Lecture du fichier impossible", e);
        }
    }

    public void delete(String storedName) {
        try {
            Files.deleteIfExists(racine.resolve(storedName).normalize());
        } catch (IOException e) {
            log.warn("Suppression de {} impossible : {}", storedName, e.getMessage());
        }
    }

    private String extension(String nomOrigine, String defaut) {
        if (nomOrigine != null && nomOrigine.contains(".")) {
            String ext = nomOrigine.substring(nomOrigine.lastIndexOf('.'));
            if (ext.length() <= 6 && ext.matches("[.][A-Za-z0-9]+")) {
                return ext.toLowerCase();
            }
        }
        return defaut;
    }
}
