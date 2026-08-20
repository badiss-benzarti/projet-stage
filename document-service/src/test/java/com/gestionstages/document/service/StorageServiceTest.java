package com.gestionstages.document.service;

import com.gestionstages.document.config.StorageProperties;
import com.gestionstages.document.exception.ApiExceptions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Le stockage est le point d'entree de fichiers venus de l'exterieur :
 * c'est la surface d'attaque la plus exposee du projet.
 */
class StorageServiceTest {

    @TempDir Path racine;

    private StorageService storage;

    @BeforeEach
    void setUp() {
        StorageProperties props = new StorageProperties();
        props.setLocation(racine.toString());
        props.setAllowedTypes(List.of("application/pdf", "image/png"));

        storage = new StorageService(props);
        storage.init();
    }

    private MockMultipartFile pdf(String nom) {
        return new MockMultipartFile("file", nom, "application/pdf", "%PDF-1.4 contenu".getBytes());
    }

    @Test
    @DisplayName("le fichier est enregistre sous un UUID, jamais sous son nom d'origine")
    void storedNameIsNeverTheOriginalName() {
        String stored = storage.store(pdf("convention signee.pdf"), ".pdf");

        assertThat(stored).doesNotContain("convention").endsWith(".pdf");
        assertThat(Files.exists(racine.resolve(stored))).isTrue();
    }

    @Test
    @DisplayName("un nom de fichier malveillant ne permet pas de sortir du repertoire")
    void pathTraversalIsNeutralised() {
        String stored = storage.store(pdf("../../../application.yml"), ".pdf");

        // Le nom d'origine est ignore : rien n'est ecrit hors du repertoire.
        assertThat(stored).doesNotContain("..").doesNotContain("/");
        assertThat(racine.resolve(stored).normalize().startsWith(racine)).isTrue();
    }

    @Test
    @DisplayName("un type MIME non autorise est refuse a l'entree")
    void disallowedMimeTypeIsRejected() {
        var script = new MockMultipartFile("file", "run.sh", "text/x-shellscript", "rm -rf".getBytes());

        assertThatThrownBy(() -> storage.store(script, ".pdf"))
                .isInstanceOf(ApiExceptions.BusinessRuleException.class)
                .hasMessageContaining("Type de fichier refuse");
    }

    @Test
    @DisplayName("un fichier vide est refuse")
    void emptyFileIsRejected() {
        var vide = new MockMultipartFile("file", "vide.pdf", "application/pdf", new byte[0]);

        assertThatThrownBy(() -> storage.store(vide, ".pdf"))
                .isInstanceOf(ApiExceptions.BusinessRuleException.class);
    }

    @Test
    @DisplayName("relire un fichier rend exactement le contenu ecrit")
    void readReturnsWhatWasWritten() {
        String stored = storage.store(pdf("rapport.pdf"), ".pdf");

        assertThat(new String(storage.read(stored))).isEqualTo("%PDF-1.4 contenu");
    }

    @Test
    @DisplayName("lire un fichier inexistant remonte une 404 metier")
    void readingMissingFileRaisesNotFound() {
        assertThatThrownBy(() -> storage.read("inexistant.pdf"))
                .isInstanceOf(ApiExceptions.NotFoundException.class);
    }

    @Test
    @DisplayName("un contenu genere en memoire est enregistre correctement")
    void generatedBytesAreStored() {
        String stored = storage.storeBytes("%PDF attestation".getBytes(), ".pdf");

        assertThat(new String(storage.read(stored))).isEqualTo("%PDF attestation");
    }

    @Test
    @DisplayName("la suppression est idempotente")
    void deleteIsIdempotent() {
        String stored = storage.store(pdf("a.pdf"), ".pdf");

        storage.delete(stored);
        storage.delete(stored);   // ne doit pas lever

        assertThat(Files.exists(racine.resolve(stored))).isFalse();
    }
}
