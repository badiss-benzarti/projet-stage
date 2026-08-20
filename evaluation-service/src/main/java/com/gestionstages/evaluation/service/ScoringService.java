package com.gestionstages.evaluation.service;

import com.gestionstages.evaluation.config.EvaluationProperties;
import com.gestionstages.evaluation.entity.Evaluation;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Calcul automatique de la note de stage.
 *
 *   note = somme(critere_i x poids_i) / 100, arrondie au pas configure
 *
 * Le calcul est isole ici, sans dependance a JPA ni a Spring Web : c'est
 * ce qui permet de le tester exhaustivement et de le defendre devant un
 * jury sans derouler toute l'application.
 */
@Service
@RequiredArgsConstructor
public class ScoringService {

    private final EvaluationProperties props;

    /**
     * @return la note finale, ou null si la grille est incomplete.
     *         Une grille partielle ne doit pas produire une note basse :
     *         elle ne doit produire aucune note.
     */
    public Double compute(Evaluation e) {
        if (!e.isComplete()) {
            return null;
        }
        var w = props.getWeights();

        double pondere =
                  e.getTechnicalScore()     * w.getTechnical()
                + e.getQualityScore()       * w.getQuality()
                + e.getAutonomyScore()      * w.getAutonomy()
                + e.getCommunicationScore() * w.getCommunication()
                + e.getPunctualityScore()   * w.getPunctuality();

        return round(pondere / w.total());
    }

    /**
     * Arrondi au pas configure (0,25 par defaut), puis a deux decimales
     * pour eviter les artefacts de virgule flottante du type 15.749999.
     */
    public double round(double valeur) {
        double pas = props.getRounding();
        double arrondie = (pas <= 0) ? valeur : Math.round(valeur / pas) * pas;

        return BigDecimal.valueOf(arrondie)
                .setScale(2, RoundingMode.HALF_UP)
                .doubleValue();
    }

    /** Detail du calcul, affiche a l'etudiant pour rendre la note lisible. */
    public Map<String, Object> breakdown(Evaluation e) {
        var w = props.getWeights();
        Map<String, Object> detail = new LinkedHashMap<>();

        detail.put("Competences techniques",      line(e.getTechnicalScore(),     w.getTechnical()));
        detail.put("Qualite du travail rendu",    line(e.getQualityScore(),       w.getQuality()));
        detail.put("Autonomie et initiative",     line(e.getAutonomyScore(),      w.getAutonomy()));
        detail.put("Communication et integration",line(e.getCommunicationScore(), w.getCommunication()));
        detail.put("Assiduite et ponctualite",    line(e.getPunctualityScore(),   w.getPunctuality()));

        return detail;
    }

    private Map<String, Object> line(Double note, double poids) {
        Map<String, Object> l = new LinkedHashMap<>();
        l.put("note", note);
        l.put("sur", props.getScale());
        l.put("poids", poids + " %");
        l.put("contribution", note == null ? null
                : BigDecimal.valueOf(note * poids / 100)
                        .setScale(2, RoundingMode.HALF_UP).doubleValue());
        return l;
    }
}
