package com.gestionstages.evaluation.service;

import com.gestionstages.evaluation.client.Lookup;
import com.gestionstages.evaluation.dto.TaskDto;
import com.gestionstages.evaluation.entity.Task;
import com.gestionstages.evaluation.enums.Role;
import com.gestionstages.evaluation.enums.TaskStatus;
import com.gestionstages.evaluation.exception.ApiExceptions;
import com.gestionstages.evaluation.repository.TaskRepository;
import com.gestionstages.evaluation.security.AuthenticatedUser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

/** Journal de stage : saisie par l'etudiant, validation par l'encadrant. */
@Slf4j
@Service
@RequiredArgsConstructor
public class TaskService {

    private final TaskRepository tasks;
    private final Lookup lookup;

    /** L'etudiant saisit une tache sur SON stage, qui doit etre en cours. */
    @Transactional
    public TaskDto.Response add(AuthenticatedUser me, Long internshipId, TaskDto.Request req) {
        var stage = lookup.internship(internshipId);
        var student = lookup.student();

        if (!stage.studentId().equals(student.id())) {
            throw new ApiExceptions.ForbiddenException("Ce stage ne vous appartient pas");
        }
        if (!"IN_PROGRESS".equals(stage.status())) {
            throw new ApiExceptions.BusinessRuleException(
                    "Le journal n'est ouvert que pendant le stage (etat actuel : " + stage.status() + ")");
        }
        if (stage.startDate() != null && req.taskDate().isBefore(stage.startDate())) {
            throw new ApiExceptions.BusinessRuleException(
                    "Cette date est anterieure au debut du stage");
        }

        Task t = Task.builder()
                .internshipId(internshipId)
                .studentId(student.id())
                .taskDate(req.taskDate())
                .title(req.title())
                .description(req.description())
                .hours(req.hours())
                .status(TaskStatus.PENDING)
                .build();

        tasks.save(t);
        return TaskDto.Response.from(t);
    }

    /** Une tache refusee redevient modifiable : c'est la boucle de correction. */
    @Transactional
    public TaskDto.Response update(AuthenticatedUser me, Long taskId, TaskDto.Request req) {
        Task t = load(taskId);

        if (!t.getStudentId().equals(lookup.student().id())) {
            throw new ApiExceptions.ForbiddenException("Cette tache ne vous appartient pas");
        }
        if (t.getStatus() == TaskStatus.VALIDATED) {
            throw new ApiExceptions.BusinessRuleException(
                    "Une tache validee ne peut plus etre modifiee");
        }

        t.setTaskDate(req.taskDate());
        t.setTitle(req.title());
        t.setDescription(req.description());
        t.setHours(req.hours());
        t.setStatus(TaskStatus.PENDING);
        t.setRejectionReason(null);

        return TaskDto.Response.from(t);
    }

    /** L'encadrant valide ou refuse. Un refus exige un motif. */
    @Transactional
    public TaskDto.Response decide(AuthenticatedUser me, Long taskId, TaskDto.Decision decision) {
        Task t = load(taskId);

        if (decision.status() == TaskStatus.PENDING) {
            throw new ApiExceptions.BusinessRuleException(
                    "Une decision doit etre VALIDATED ou REJECTED");
        }
        if (decision.status() == TaskStatus.REJECTED
                && (decision.reason() == null || decision.reason().isBlank())) {
            throw new ApiExceptions.BusinessRuleException("Un motif est obligatoire pour refuser");
        }

        var stage = lookup.internship(t.getInternshipId());
        if (Role.of(me.role()) == Role.ENCADRANT) {
            var sup = lookup.supervisor();
            if (stage.supervisorId() == null || !stage.supervisorId().equals(sup.id())) {
                throw new ApiExceptions.ForbiddenException("Vous n'encadrez pas ce stagiaire");
            }
        }

        t.setStatus(decision.status());
        t.setRejectionReason(decision.status() == TaskStatus.REJECTED ? decision.reason() : null);
        t.setValidatedBy(me.fullName());
        t.setValidatedAt(Instant.now());

        log.info("Tache {} {} par {}", taskId, decision.status(), me.email());
        return TaskDto.Response.from(t);
    }

    @Transactional
    public void delete(AuthenticatedUser me, Long taskId) {
        Task t = load(taskId);
        if (!t.getStudentId().equals(lookup.student().id())) {
            throw new ApiExceptions.ForbiddenException("Cette tache ne vous appartient pas");
        }
        if (t.getStatus() == TaskStatus.VALIDATED) {
            throw new ApiExceptions.BusinessRuleException("Une tache validee ne peut pas etre supprimee");
        }
        tasks.delete(t);
    }

    @Transactional(readOnly = true)
    public Page<TaskDto.Response> list(Long internshipId, TaskStatus status, Pageable pageable) {
        Page<Task> page = (status == null)
                ? tasks.findByInternshipId(internshipId, pageable)
                : tasks.findByInternshipIdAndStatus(internshipId, status, pageable);
        return page.map(TaskDto.Response::from);
    }

    @Transactional(readOnly = true)
    public List<Task> allOf(Long internshipId) {
        return tasks.findByInternshipIdOrderByTaskDateAsc(internshipId);
    }

    /** Synthese reutilisee par le PDF, le tableau de bord et le modele ML. */
    @Transactional(readOnly = true)
    public TaskDto.Summary summary(Long internshipId) {
        long pending   = tasks.countByInternshipIdAndStatus(internshipId, TaskStatus.PENDING);
        long validated = tasks.countByInternshipIdAndStatus(internshipId, TaskStatus.VALIDATED);
        long rejected  = tasks.countByInternshipIdAndStatus(internshipId, TaskStatus.REJECTED);
        Double heures  = tasks.sumValidatedHours(internshipId);
        LocalDate last = tasks.lastTaskDate(internshipId);

        return new TaskDto.Summary(pending + validated + rejected, pending, validated, rejected,
                heures == null ? 0d : heures, last);
    }

    private Task load(Long id) {
        return tasks.findById(id)
                .orElseThrow(() -> new ApiExceptions.NotFoundException("Tache", id));
    }
}
