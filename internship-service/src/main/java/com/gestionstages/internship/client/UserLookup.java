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

    /**
     * Encadrants declares par une entreprise.
     *
     * Une entreprise sans encadrant declare renvoie une liste vide, ce qui
     * n'est pas une panne : le service appelant en tire un message metier.
     */
    public java.util.List<UserClient.SupervisorOption> supervisorOptions(Long companyId) {
        try {
            return users.supervisorOptions(companyId, bearer());
        } catch (Exception e) {
            log.warn("Liste des encadrants de l'entreprise {} indisponible : {}", companyId, e.getMessage());
            throw new ApiExceptions.BusinessRuleException(
                    "Impossible de recuperer les encadrants de cette entreprise pour le moment");
        }
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
