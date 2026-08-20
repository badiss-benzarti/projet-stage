package com.gestionstages.document.client;

import com.gestionstages.document.exception.ApiExceptions;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.function.Supplier;

/** Retransmet le jeton de la requete en cours aux services appeles. */
@Slf4j
@Component
@RequiredArgsConstructor
public class Lookup {

    private final UserClient users;
    private final InternshipClient internships;

    public UserClient.Ref student() {
        return call(() -> users.myStudentProfile(bearer()), "profil etudiant");
    }

    public InternshipClient.InternshipRef internship(Long id) {
        return call(() -> internships.byId(id, bearer()), "stage " + id);
    }

    private <T> T call(Supplier<T> supplier, String quoi) {
        try {
            return supplier.get();
        } catch (Exception e) {
            log.warn("Resolution de {} impossible : {}", quoi, e.getMessage());
            throw new ApiExceptions.BusinessRuleException("Impossible de recuperer : " + quoi);
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
