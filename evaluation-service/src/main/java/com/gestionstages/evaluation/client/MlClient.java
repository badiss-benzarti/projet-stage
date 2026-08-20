package com.gestionstages.evaluation.client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

/**
 * Appel du service de prediction (Python / FastAPI).
 *
 * RestClient et non Feign : ml-service n'est pas un service Spring Cloud,
 * il ne s'enregistre pas dans Eureka. Son URL vient du config-repo.
 *
 * Une panne du modele ne doit jamais casser une page : l'echec renvoie
 * un resultat "indisponible" plutot qu'une erreur HTTP.
 */
@Slf4j
@Component
public class MlClient {

    private final RestClient client;

    public MlClient(@Value("${app.ml.url}") String baseUrl) {
        this.client = RestClient.builder().baseUrl(baseUrl).build();
    }

    public Prediction predict(Features features) {
        try {
            Prediction p = client.post()
                    .uri("/predict")
                    .body(features)
                    .retrieve()
                    .body(Prediction.class);
            return p == null ? indisponible() : p;
        } catch (Exception e) {
            log.warn("Service de prediction injoignable : {}", e.getMessage());
            return indisponible();
        }
    }

    private Prediction indisponible() {
        return new Prediction("UNAVAILABLE", 0d, Map.of(),
                List.of("Service de prediction momentanement indisponible"));
    }

    /** Indicateurs envoyes au modele. Aucune donnee nominative. */
    public record Features(
            int tasks_total,
            int tasks_pending,
            int tasks_rejected,
            double hours_ratio,
            int days_since_last_entry,
            double progress_ratio
    ) {}

    public record Prediction(
            String risk,
            Double probability,
            Map<String, Double> probabilities,
            List<String> drivers
    ) {}
}
