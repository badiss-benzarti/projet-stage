package com.gestionstages.evaluation.config;

import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Bareme de la grille d'evaluation, servi par le config-server
 * (config-repo/evaluation-service.yml).
 *
 * Externaliser le bareme evite de recompiler pour changer un coefficient,
 * et permet de le montrer au jury sans ouvrir le code.
 */
@Slf4j
@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "app.evaluation")
public class EvaluationProperties {

    private Weights weights = new Weights();

    /** Chaque critere est note sur cette valeur. */
    private double scale = 20d;

    /** Pas d'arrondi de la note finale. */
    private double rounding = 0.25d;

    @Getter @Setter
    public static class Weights {
        private double technical = 30;
        private double quality = 20;
        private double autonomy = 20;
        private double communication = 15;
        private double punctuality = 15;

        public double total() {
            return technical + quality + autonomy + communication + punctuality;
        }
    }

    /**
     * Echoue au demarrage si les poids ne totalisent pas 100.
     *
     * Une coquille dans le YAML donnerait des notes silencieusement
     * fausses : mieux vaut refuser de demarrer.
     */
    @PostConstruct
    public void verifierBareme() {
        double total = weights.total();
        if (Math.abs(total - 100d) > 0.001) {
            throw new IllegalStateException(
                    "Les poids de la grille doivent totaliser 100, trouve : " + total);
        }
        log.info("Bareme charge : technique {}%, qualite {}%, autonomie {}%, "
                        + "communication {}%, assiduite {}% (note sur {}, arrondi {})",
                weights.technical, weights.quality, weights.autonomy,
                weights.communication, weights.punctuality, scale, rounding);
    }
}
