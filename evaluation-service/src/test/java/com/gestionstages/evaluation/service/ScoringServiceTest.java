package com.gestionstages.evaluation.service;

import com.gestionstages.evaluation.config.EvaluationProperties;
import com.gestionstages.evaluation.entity.Evaluation;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Tests du calcul de la note. C'est la partie du projet ou une erreur
 * coute le plus cher : une note fausse est invisible tant qu'on ne la
 * verifie pas a la main.
 */
class ScoringServiceTest {

    private EvaluationProperties props;
    private ScoringService scoring;

    @BeforeEach
    void setUp() {
        props = new EvaluationProperties();   // 30/20/20/15/15, arrondi 0,25
        scoring = new ScoringService(props);
    }

    private Evaluation grille(Double tech, Double qual, Double auto, Double comm, Double ponc) {
        return Evaluation.builder()
                .technicalScore(tech).qualityScore(qual).autonomyScore(auto)
                .communicationScore(comm).punctualityScore(ponc)
                .build();
    }

    @Test
    @DisplayName("cas de reference : 16/15/16/15/16 donne 15,75")
    void referenceCase() {
        // (16x30 + 15x20 + 16x20 + 15x15 + 16x15) / 100 = 15,65 -> 15,75
        assertThat(scoring.compute(grille(16d, 15d, 16d, 15d, 16d))).isEqualTo(15.75);
    }

    @Test
    @DisplayName("une grille pleine a 20 donne exactement 20")
    void perfectScore() {
        assertThat(scoring.compute(grille(20d, 20d, 20d, 20d, 20d))).isEqualTo(20.0);
    }

    @Test
    @DisplayName("une grille a zero donne zero, pas null")
    void zeroIsAValidScore() {
        assertThat(scoring.compute(grille(0d, 0d, 0d, 0d, 0d))).isEqualTo(0.0);
    }

    @Test
    @DisplayName("une grille incomplete ne produit AUCUNE note, pas une note basse")
    void incompleteGridProducesNoScore() {
        assertThat(scoring.compute(grille(16d, 15d, null, 15d, 16d))).isNull();
        assertThat(scoring.compute(grille(null, null, null, null, null))).isNull();
    }

    @Test
    @DisplayName("le critere technique pese bien 30 pour cent")
    void technicalWeightsMost() {
        double avecTech20 = scoring.compute(grille(20d, 10d, 10d, 10d, 10d));
        double avecPonc20 = scoring.compute(grille(10d, 10d, 10d, 10d, 20d));

        // 10 points de plus sur un critere a 30 % rapportent 3 points ;
        // sur un critere a 15 %, seulement 1,5 point.
        assertThat(avecTech20).isEqualTo(13.0);
        assertThat(avecPonc20).isEqualTo(11.5);
    }

    @ParameterizedTest(name = "{0} arrondi a {1}")
    @CsvSource({
            "15.60, 15.50",
            "15.65, 15.75",
            "15.62, 15.50",
            "15.63, 15.75",
            "15.87, 15.75",
            "15.88, 16.00",
            "12.125, 12.25",
            "19.99, 20.00"
    })
    @DisplayName("arrondi au quart de point le plus proche")
    void roundingToQuarterPoint(double brut, double attendu) {
        assertThat(scoring.round(brut)).isEqualTo(attendu);
    }

    @Test
    @DisplayName("pas d artefact de virgule flottante du type 15.749999999")
    void noFloatingPointArtefacts() {
        for (double v = 0; v <= 20; v += 0.13) {
            double r = scoring.round(v);
            assertThat(BigDecimalHelper.decimals(r))
                    .as("valeur arrondie %s issue de %s", r, v)
                    .isLessThanOrEqualTo(2);
        }
    }

    @Test
    @DisplayName("le detail du calcul couvre les cinq criteres")
    void breakdownCoversEveryCriterion() {
        var detail = scoring.breakdown(grille(16d, 15d, 16d, 15d, 16d));

        assertThat(detail).hasSize(5)
                .containsKeys("Competences techniques", "Qualite du travail rendu",
                        "Autonomie et initiative", "Communication et integration",
                        "Assiduite et ponctualite");
    }

    @Test
    @DisplayName("un bareme dont les poids ne totalisent pas 100 empeche le demarrage")
    void invalidWeightsFailFast() {
        EvaluationProperties casse = new EvaluationProperties();
        casse.getWeights().setTechnical(50);   // total = 120

        assertThatThrownBy(casse::verifierBareme)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("100");
    }

    /** Compte les decimales significatives d'un double. */
    static class BigDecimalHelper {
        static int decimals(double v) {
            return java.math.BigDecimal.valueOf(v).stripTrailingZeros().scale();
        }
    }
}
