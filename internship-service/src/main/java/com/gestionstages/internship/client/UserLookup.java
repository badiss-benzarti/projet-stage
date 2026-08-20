package com.gestionstages.internship.client;

import com.gestionstages.internship.exception.ApiExceptions;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * Enveloppe le UserClient : recupere le jeton de la requete en cours et
 * transforme une panne de user-service en erreur metier lisible plutot
 * qu'en trace Feign de cinquante lignes.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class UserLookup {

    private final UserClient users;

    public UserClient.StudentRef student() {
        return call(() -> users.myStudentProfile(bearer()), "etudiant");
    }

    public UserClient.CompanyRef company() {
        return call(() -> users.myCompanyProfile(bearer()), "entreprise");
    }

    public UserClient.SupervisorRef supervisor() {
        return call(() -> users.mySupervisorProfile(bearer()), "encadrant");
    }

    private <T> T call(java.util.function.Supplier<T> supplier, String profil) {
        try {
            return supplier.get();
        } catch (Exception e) {
            log.warn("Resolution du profil {} impossible : {}", profil, e.getMessage());
            throw new ApiExceptions.BusinessRuleException(
                    "Completez d'abord votre profil " + profil + " avant d'utiliser cette fonctionnalite");
        }
    }

    private String bearer() {
        var attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attrs == null) {
            throw new ApiExceptions.BusinessRuleException("Contexte de requete indisponible");
        }
        String header = attrs.getRequest().getHeader("Authorization");
        if (header == null || header.isBlank()) {
            throw new ApiExceptions.ForbiddenException("Jeton absent");
        }
        return header;
    }
}
