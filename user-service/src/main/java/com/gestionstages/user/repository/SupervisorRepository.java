package com.gestionstages.user.repository;

import com.gestionstages.user.entity.Supervisor;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SupervisorRepository extends JpaRepository<Supervisor, Long> {

    Optional<Supervisor> findByUserId(Long userId);

    boolean existsByUserId(Long userId);

    List<Supervisor> findByCompanyId(Long companyId);
}
