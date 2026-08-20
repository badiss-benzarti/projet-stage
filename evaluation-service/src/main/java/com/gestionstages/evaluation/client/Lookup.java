package com.gestionstages.evaluation.client;

import com.gestionstages.evaluation.exception.ApiExceptions;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.function.Supplier;

/**
 * Recupere le jeton de la requete en cours et le retransmet aux services
 * appeles. Une panne distante devient une erreur metier lisible plutot
 * qu'une trace Feign.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class Lookup {

    private final UserClient users;
    private final InternshipClient internships;

    public UserClient.Ref student() {
        return call(() -> users.myStudentProfile(bearer()), "etudiant");
    }

    public UserClient.Ref supervisor() {
        return call(() -> users.mySupervisorProfile(bearer()), "encadrant");
    }

    public InternshipClient.InternshipRef internship(Long id) {
        return call(() -> internships.byId(id, bearer()), "stage " + id);
    }

    private <T> T call(Supplier<T> supplier, String quoi) {
        try {
            return supplier.get();
        } catch (Exception e) {
            log.warn("Resolution de {} impossible : {}", quoi, e.getMessage());
            throw new ApiExceptions.BusinessRuleException(
                    "Impossible de recuperer les informations : " + quoi);
        }
    }

    public String bearer() {
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
