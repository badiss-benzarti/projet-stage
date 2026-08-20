package com.gestionstages.evaluation.controller;

import com.gestionstages.evaluation.service.RiskService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Prediction du risque de difficulte pendant le stage.
 *
 * Reserve aux encadrants et aux chefs de departement : afficher a un
 * etudiant qu'un algorithme le classe "a risque eleve" serait contre-
 * productif, et discutable ethiquement.
 */
@RestController
@RequestMapping("/api/evaluations")
@RequiredArgsConstructor
public class RiskController {

    private final RiskService risk;

    @GetMapping("/internships/{internshipId}/risk")
    @PreAuthorize("hasAnyRole('ENCADRANT','CHEF_DEPARTEMENT_STAGE','CHEF_DEPARTEMENT_PEDAGOGIQUE','ADMIN')")
    public Map<String, Object> assess(@PathVariable Long internshipId) {
        return risk.assess(internshipId);
    }
}
