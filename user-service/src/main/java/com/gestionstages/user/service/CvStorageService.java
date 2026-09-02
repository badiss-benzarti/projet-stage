package com.gestionstages.user.service;

import com.gestionstages.user.config.CvProperties;
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
 * CV des etudiants sur disque.
 *
 * Meme precaution que pour les photos : le nom d'origine n'est jamais
 * utilise comme nom de fichier, un "../../application.yml" permettrait
 * d'ecrire hors du repertoire. Il est conserve en base, pour l'affichage
 * et pour nommer le telechargement, mais le fichier porte un UUID.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CvStorageService {

    private final CvProperties props;
    private Path racine;

    @PostConstruct
    void init() {
        this.racine = Paths.get(props.getLocation()).toAbsolutePath().normalize();
        try {
            Files.createDirectories(racine);
            log.info("Repertoire des CV : {}", racine);
        } catch (IOException e) {
            throw new IllegalStateException("Repertoire des CV inaccessible : " + racine, e);
        }
    }

    /** @return le nom sous lequel le CV a ete enregistre. */
    public String store(MultipartFile fichier) {
        if (fichier == null || fichier.isEmpty()) {
            throw new ApiExceptions.BusinessRuleException("Le fichier est vide");
        }
        if (!props.getAllowedTypes().contains(fichier.getContentType())) {
            throw new ApiExceptions.BusinessRuleException(
                    "Votre CV doit etre un PDF. Format recu : " + fichier.getContentType());
        }
        if (fichier.getSize() > props.getMaxSizeBytes()) {
            throw new ApiExceptions.BusinessRuleException(
                    "CV trop lourd : " + (props.getMaxSizeBytes() / 1024 / 1024) + " Mo maximum");
        }

        String nom = UUID.randomUUID() + ".pdf";
        Path cible = racine.resolve(nom).normalize();

        if (!cible.startsWith(racine)) {
            throw new ApiExceptions.BusinessRuleException("Chemin de destination invalide");
        }

        try (var in = fichier.getInputStream()) {
            Files.copy(in, cible, StandardCopyOption.REPLACE_EXISTING);
            return nom;
        } catch (IOException e) {
            throw new IllegalStateException("Ecriture du CV impossible", e);
        }
    }

    public byte[] read(String nom) {
        Path fichier = racine.resolve(nom).normalize();
        if (!fichier.startsWith(racine) || !Files.exists(fichier)) {
            throw new ApiExceptions.NotFoundException("CV", nom);
        }
        try {
            return Files.readAllBytes(fichier);
        } catch (IOException e) {
            throw new IllegalStateException("Lecture du CV impossible", e);
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

    /**
     * Nom propose au telechargement. On repart du nom d'origine mais on
     * le desinfecte : il vient de l'utilisateur et finit dans un en-tete
     * Content-Disposition.
     */
    public String downloadName(String nomOrigine, String nomEtudiant) {
        if (nomOrigine == null || nomOrigine.isBlank()) {
            return "cv-" + nomEtudiant.replaceAll("[^A-Za-z0-9]+", "-").toLowerCase() + ".pdf";
        }
        String propre = nomOrigine.replaceAll("[\\r\\n\"\\\\/]", "").trim();
        return propre.isBlank() ? "cv.pdf" : propre;
    }
}
