package com.gestionstages.evaluation.service;

import com.gestionstages.evaluation.client.Lookup;
import com.gestionstages.evaluation.dto.EvaluationDto;
import com.gestionstages.evaluation.entity.Evaluation;
import com.gestionstages.evaluation.enums.EvaluationStatus;
import com.gestionstages.evaluation.enums.Role;
import com.gestionstages.evaluation.exception.ApiExceptions;
import com.gestionstages.evaluation.repository.EvaluationRepository;
import com.gestionstages.evaluation.security.AuthenticatedUser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * Grille d'evaluation et note de stage.
 *
 * La note n'est jamais saisie : elle est recalculee a chaque
 * enregistrement par ScoringService. Un encadrant ne peut donc pas
 * imposer une note incoherente avec sa propre grille.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EvaluationService {

    private final EvaluationRepository evaluations;
    private final ScoringService scoring;
    private final Lookup lookup;

    /** Cree ou met a jour la grille. Une grille validee est figee. */
    @Transactional
    public EvaluationDto.Response save(AuthenticatedUser me, Long internshipId,
                                       EvaluationDto.Request req) {
        var stage = lookup.internship(internshipId);
        requireSupervisorOf(me, stage.supervisorId());

        Evaluation e = evaluations.findByInternshipId(internshipId).orElseGet(() ->
                Evaluation.builder()
                        .internshipId(internshipId)
                        .studentId(stage.studentId())
                        .studentName(stage.studentName())
                        .supervisorId(stage.supervisorId())
                        .supervisorName(stage.supervisorName())
                        .companyName(stage.companyName())
                        .internshipType(stage.type())
                        .status(EvaluationStatus.DRAFT)
                        .build());

        if (e.getStatus() == EvaluationStatus.SUBMITTED) {
            throw new ApiExceptions.BusinessRuleException(
                    "Cette grille est validee, la note est definitive");
        }

        e.setTechnicalScore(req.technicalScore());
        e.setQualityScore(req.qualityScore());
        e.setAutonomyScore(req.autonomyScore());
        e.setCommunicationScore(req.communicationScore());
        e.setPunctualityScore(req.punctualityScore());
        e.setGlobalComment(req.globalComment());
        e.setRemarks(req.remarks());

        // La note est TOUJOURS recalculee, jamais lue depuis la requete.
        e.setFinalScore(scoring.compute(e));

        evaluations.save(e);
        log.info("Grille du stage {} enregistree, note {}", internshipId, e.getFinalScore());

        return EvaluationDto.Response.from(e, scoring.breakdown(e));
    }

    /**
     * Validation definitive : la note devient visible pour l'etudiant et
     * ne peut plus changer, sauf par le biais d'une reclamation.
     */
    @Transactional
    public EvaluationDto.Response submit(AuthenticatedUser me, Long internshipId) {
        Evaluation e = load(internshipId);
        requireSupervisorOf(me, e.getSupervisorId());

        if (e.getStatus() == EvaluationStatus.SUBMITTED) {
            throw new ApiExceptions.BusinessRuleException("Grille deja validee");
        }
        if (!e.isComplete()) {
            throw new ApiExceptions.BusinessRuleException(
                    "Renseignez les cinq criteres avant de valider");
        }
        if (e.getGlobalComment() == null || e.getGlobalComment().isBlank()) {
            throw new ApiExceptions.BusinessRuleException(
                    "L'appreciation globale est obligatoire");
        }

        e.setFinalScore(scoring.compute(e));
        e.setStatus(EvaluationStatus.SUBMITTED);
        e.setSubmittedAt(Instant.now());

        log.info("Grille du stage {} validee, note definitive {}", internshipId, e.getFinalScore());
        return EvaluationDto.Response.from(e, scoring.breakdown(e));
    }

    @Transactional(readOnly = true)
    public EvaluationDto.Response findByInternship(AuthenticatedUser me, Long internshipId) {
        Evaluation e = load(internshipId);
        Role role = Role.of(me.role());

        // L'etudiant ne voit sa note qu'une fois la grille validee.
        if (role == Role.ETUDIANT && e.getStatus() != EvaluationStatus.SUBMITTED) {
            throw new ApiExceptions.ForbiddenException(
                    "Votre evaluation n'est pas encore finalisee");
        }
        return EvaluationDto.Response.from(e, scoring.breakdown(e));
    }

    /** Toutes les notes definitives, triees par etudiant : base de l'export XLSX. */
    @Transactional(readOnly = true)
    public List<Evaluation> allSubmitted() {
        return evaluations.findByStatusOrderByStudentNameAsc(EvaluationStatus.SUBMITTED);
    }

    /** Statistiques pour le tableau de bord du departement pedagogique. */
    @Transactional(readOnly = true)
    public Map<String, Object> statistics() {
        List<Evaluation> notes = allSubmitted();
        if (notes.isEmpty()) {
            return Map.of("count", 0, "average", 0d, "min", 0d, "max", 0d);
        }
        var stats = notes.stream()
                .map(Evaluation::getFinalScore)
                .filter(java.util.Objects::nonNull)
                .mapToDouble(Double::doubleValue)
                .summaryStatistics();

        return Map.of(
                "count",   stats.getCount(),
                "average", scoring.round(stats.getAverage()),
                "min",     stats.getMin(),
                "max",     stats.getMax());
    }

    private Evaluation load(Long internshipId) {
        return evaluations.findByInternshipId(internshipId)
                .orElseThrow(() -> new ApiExceptions.NotFoundException("Evaluation du stage", internshipId));
    }

    /** Seul l'encadrant designe sur le stage peut noter. */
    private void requireSupervisorOf(AuthenticatedUser me, Long supervisorId) {
        Role role = Role.of(me.role());
        if (role == Role.ADMIN || role == Role.CHEF_DEPARTEMENT_PEDAGOGIQUE) {
            return;
        }
        if (role != Role.ENCADRANT) {
            throw new ApiExceptions.ForbiddenException("Seul l'encadrant remplit la grille");
        }
        var sup = lookup.supervisor();
        if (supervisorId == null || !supervisorId.equals(sup.id())) {
            throw new ApiExceptions.ForbiddenException("Vous n'encadrez pas ce stagiaire");
        }
    }
}
