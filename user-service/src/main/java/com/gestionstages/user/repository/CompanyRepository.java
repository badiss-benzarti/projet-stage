package com.gestionstages.user.repository;

import com.gestionstages.user.entity.Company;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CompanyRepository extends JpaRepository<Company, Long> {

    Optional<Company> findByUserId(Long userId);

    boolean existsByUserId(Long userId);

    boolean existsByNameIgnoreCase(String name);
}
