package com.gestionstages.evaluation.service;

import com.gestionstages.evaluation.client.InternshipClient;
import com.gestionstages.evaluation.client.Lookup;
import com.gestionstages.evaluation.client.MlClient;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Traduit le journal de stage en indicateurs, puis interroge le modele.
 *
 * Tout le travail est ici : le modele ne sait rien du metier, il ne voit
 * que six nombres. C'est cette classe qui garantit que ces nombres ont
 * un sens.
 */
@Service
@RequiredArgsConstructor
public class RiskService {

    private final TaskService tasks;
    private final MlClient ml;
    private final Lookup lookup;

    /** Volume horaire quotidien attendu, moyenne lissee sur la semaine. */
    @Value("${app.evaluation.expected-hours-per-day:5.0}")
    private double heuresParJour;

    @Transactional(readOnly = true)
    public Map<String, Object> assess(Long internshipId) {
        InternshipClient.InternshipRef stage = lookup.internship(internshipId);
        var journal = tasks.summary(internshipId);
        LocalDate today = LocalDate.now();

        double avancement = avancement(stage.startDate(), stage.endDate(), today);
        double ratioHeures = ratioHeures(journal.validatedHours(), stage.startDate(), today);
        int silence = silence(journal.lastEntry(), stage.startDate(), today);

        var features = new MlClient.Features(
                (int) journal.total(),
                (int) journal.pending(),
                (int) journal.rejected(),
                arrondir(ratioHeures),
                silence,
                arrondir(avancement));

        MlClient.Prediction prediction = ml.predict(features);

        Map<String, Object> reponse = new LinkedHashMap<>();
        reponse.put("internshipId", internshipId);
        reponse.put("studentName", stage.studentName());
        reponse.put("risk", prediction.risk());
        reponse.put("probability", prediction.probability());
        reponse.put("probabilities", prediction.probabilities());
        reponse.put("drivers", prediction.drivers());
        reponse.put("features", features);
        return reponse;
    }

    /** Part du stage deja ecoulee, bornee entre 0 et 1. */
    private double avancement(LocalDate debut, LocalDate fin, LocalDate today) {
        if (debut == null || fin == null || !fin.isAfter(debut)) {
            return 0d;
        }
        double duree = ChronoUnit.DAYS.between(debut, fin);
        double ecoule = ChronoUnit.DAYS.between(debut, today);
        return Math.clamp(ecoule / duree, 0d, 1d);
    }

    /**
     * Heures validees rapportees aux heures attendues DEPUIS LE DEBUT,
     * et non sur la duree totale : un stage a mi-parcours ne doit pas
     * apparaitre en retard simplement parce qu'il n'est pas fini.
     */
    private double ratioHeures(double heuresValidees, LocalDate debut, LocalDate today) {
        if (debut == null || !today.isAfter(debut)) {
            return 1d;
        }
        double attendues = ChronoUnit.DAYS.between(debut, today) * heuresParJour;
        if (attendues <= 0) {
            return 1d;
        }
        return Math.clamp(heuresValidees / attendues, 0d, 3d);
    }

    /** Jours sans saisie. Sans aucune tache, on compte depuis le debut du stage. */
    private int silence(LocalDate derniereSaisie, LocalDate debut, LocalDate today) {
        LocalDate reference = derniereSaisie != null ? derniereSaisie : debut;
        if (reference == null || reference.isAfter(today)) {
            return 0;
        }
        return (int) Math.min(ChronoUnit.DAYS.between(reference, today), 60);
    }

    private double arrondir(double v) {
        return Math.round(v * 100d) / 100d;
    }
}
