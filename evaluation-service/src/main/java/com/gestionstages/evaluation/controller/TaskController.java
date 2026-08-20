package com.gestionstages.evaluation.controller;

import com.gestionstages.evaluation.dto.TaskDto;
import com.gestionstages.evaluation.enums.TaskStatus;
import com.gestionstages.evaluation.security.AuthenticatedUser;
import com.gestionstages.evaluation.service.TaskService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/** Journal de stage en ligne. */
@RestController
@RequestMapping("/api/evaluations")
@RequiredArgsConstructor
public class TaskController {

    private final TaskService tasks;

    @PostMapping("/internships/{internshipId}/tasks")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ETUDIANT')")
    public TaskDto.Response add(@AuthenticationPrincipal AuthenticatedUser me,
                                @PathVariable Long internshipId,
                                @Valid @RequestBody TaskDto.Request request) {
        return tasks.add(me, internshipId, request);
    }

    @GetMapping("/internships/{internshipId}/tasks")
    public Page<TaskDto.Response> list(@PathVariable Long internshipId,
                                       @RequestParam(required = false) TaskStatus status,
                                       @PageableDefault(size = 50, sort = "taskDate") Pageable pageable) {
        return tasks.list(internshipId, status, pageable);
    }

    @GetMapping("/internships/{internshipId}/tasks/summary")
    public TaskDto.Summary summary(@PathVariable Long internshipId) {
        return tasks.summary(internshipId);
    }

    @PutMapping("/tasks/{taskId}")
    @PreAuthorize("hasRole('ETUDIANT')")
    public TaskDto.Response update(@AuthenticationPrincipal AuthenticatedUser me,
                                   @PathVariable Long taskId,
                                   @Valid @RequestBody TaskDto.Request request) {
        return tasks.update(me, taskId, request);
    }

    @DeleteMapping("/tasks/{taskId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('ETUDIANT')")
    public void delete(@AuthenticationPrincipal AuthenticatedUser me, @PathVariable Long taskId) {
        tasks.delete(me, taskId);
    }

    /** Validation ou refus par l'encadrant d'entreprise. */
    @PatchMapping("/tasks/{taskId}/decision")
    @PreAuthorize("hasAnyRole('ENCADRANT','ADMIN')")
    public TaskDto.Response decide(@AuthenticationPrincipal AuthenticatedUser me,
                                   @PathVariable Long taskId,
                                   @Valid @RequestBody TaskDto.Decision decision) {
        return tasks.decide(me, taskId, decision);
    }
}
