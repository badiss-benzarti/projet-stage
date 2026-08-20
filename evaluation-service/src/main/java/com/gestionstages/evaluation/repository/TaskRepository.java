package com.gestionstages.evaluation.repository;

import com.gestionstages.evaluation.entity.Task;
import com.gestionstages.evaluation.enums.TaskStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface TaskRepository extends JpaRepository<Task, Long> {

    List<Task> findByInternshipIdOrderByTaskDateAsc(Long internshipId);

    Page<Task> findByInternshipId(Long internshipId, Pageable pageable);

    Page<Task> findByInternshipIdAndStatus(Long internshipId, TaskStatus status, Pageable pageable);

    long countByInternshipIdAndStatus(Long internshipId, TaskStatus status);

    @Query("select coalesce(sum(t.hours), 0) from Task t "
         + "where t.internshipId = :internshipId and t.status = com.gestionstages.evaluation.enums.TaskStatus.VALIDATED")
    Double sumValidatedHours(Long internshipId);

    @Query("select max(t.taskDate) from Task t where t.internshipId = :internshipId")
    java.time.LocalDate lastTaskDate(Long internshipId);
}
