package com.gestionstages.evaluation.service;

import com.gestionstages.evaluation.client.Lookup;
import com.gestionstages.evaluation.dto.ClaimDto;
import com.gestionstages.evaluation.entity.Claim;
import com.gestionstages.evaluation.entity.ClaimMessage;
import com.gestionstages.evaluation.enums.ClaimStatus;
import com.gestionstages.evaluation.enums.Role;
import com.gestionstages.evaluation.exception.ApiExceptions;
import com.gestionstages.evaluation.repository.ClaimRepository;
import com.gestionstages.evaluation.security.AuthenticatedUser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

/**
 * Reclamations avec bouclage.
 *
 * Le cahier des charges exige le "bouclage" : un aller-retour, pas un
 * formulaire a sens unique. Concretement, l'etudiant peut relancer apres
 * chaque reponse, et seul le chef de departement pedagogique cloture.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ClaimService {

    private final ClaimRepository claims;
    private final Lookup lookup;

    @Transactional
    public ClaimDto.Response open(AuthenticatedUser me, ClaimDto.Request req) {
        var student = lookup.student();
        var stage = lookup.internship(req.internshipId());

        if (!stage.studentId().equals(student.id())) {
            throw new ApiExceptions.ForbiddenException("Ce stage ne vous appartient pas");
        }

        Claim c = Claim.builder()
                .internshipId(req.internshipId())
                .studentId(student.id())
                .studentName(student.fullName())
                .type(req.type())
                .subject(req.subject())
                .status(ClaimStatus.OPEN)
                .build();

        c.getMessages().add(message(c, me, req.message()));
        claims.save(c);

        log.info("Reclamation {} ouverte par {}", c.getId(), me.email());
        return detail(c);
    }

    /**
     * Ajoute un message et fait avancer le fil.
     *
     * Le nouvel etat depend de QUI parle : une reponse du departement
     * passe en RESPONDED, une relance de l'etudiant en REOPENED. C'est ce
     * va-et-vient qui constitue le bouclage.
     */
    @Transactional
    public ClaimDto.Response reply(AuthenticatedUser me, Long claimId, ClaimDto.MessageRequest req) {
        Claim c = loadWithMessages(claimId);
        Role role = Role.of(me.role());

        if (c.getStatus().isClosed()) {
            throw new ApiExceptions.BusinessRuleException(
                    "Cette reclamation est close, ouvrez-en une nouvelle si besoin");
        }
        requireParticipant(c, me, role);

        c.getMessages().add(message(c, me, req.content()));

        if (role == Role.ETUDIANT) {
            // Relance apres une reponse : on incremente le compteur de bouclage.
            if (c.getStatus() == ClaimStatus.RESPONDED) {
                c.setStatus(ClaimStatus.REOPENED);
                c.setReopenCount(c.getReopenCount() + 1);
            }
        } else {
            c.setStatus(ClaimStatus.RESPONDED);
        }

        return detail(c);
    }

    /** Prise en charge par le departement pedagogique. */
    @Transactional
    public ClaimDto.Response take(AuthenticatedUser me, Long claimId) {
        Claim c = loadWithMessages(claimId);
        if (c.getStatus() != ClaimStatus.OPEN && c.getStatus() != ClaimStatus.REOPENED) {
            throw new ApiExceptions.BusinessRuleException(
                    "Cette reclamation est deja prise en charge (" + c.getStatus() + ")");
        }
        c.setStatus(ClaimStatus.IN_REVIEW);
        return detail(c);
    }

    /** Cloture definitive : reservee au departement pedagogique. */
    @Transactional
    public ClaimDto.Response close(AuthenticatedUser me, Long claimId, ClaimDto.MessageRequest req) {
        Claim c = loadWithMessages(claimId);

        if (c.getStatus().isClosed()) {
            throw new ApiExceptions.BusinessRuleException("Reclamation deja close");
        }
        if (req != null && req.content() != null && !req.content().isBlank()) {
            c.getMessages().add(message(c, me, req.content()));
        }

        c.setStatus(ClaimStatus.CLOSED);
        c.setClosedAt(Instant.now());

        log.info("Reclamation {} close apres {} relance(s)", claimId, c.getReopenCount());
        return detail(c);
    }

    @Transactional(readOnly = true)
    public ClaimDto.Response findById(AuthenticatedUser me, Long claimId) {
        Claim c = loadWithMessages(claimId);
        requireParticipant(c, me, Role.of(me.role()));
        return detail(c);
    }

    @Transactional(readOnly = true)
    public Page<ClaimDto.Response> mine(AuthenticatedUser me, Pageable pageable) {
        return claims.findByStudentId(lookup.student().id(), pageable)
                .map(ClaimDto.Response::summary);
    }

    @Transactional(readOnly = true)
    public Page<ClaimDto.Response> forDepartment(ClaimStatus status, Pageable pageable) {
        Page<Claim> page = (status == null)
                ? claims.findByStatusNot(ClaimStatus.CLOSED, pageable)
                : claims.findByStatus(status, pageable);
        return page.map(ClaimDto.Response::summary);
    }

    // ---- utilitaires ----

    private ClaimMessage message(Claim c, AuthenticatedUser me, String content) {
        return ClaimMessage.builder()
                .claim(c)
                .authorId(me.id())
                .authorName(me.fullName())
                .authorRole(me.role())
                .content(content)
                .build();
    }

    private ClaimDto.Response detail(Claim c) {
        List<ClaimDto.Message> messages = c.getMessages().stream()
                .map(m -> new ClaimDto.Message(m.getId(), m.getAuthorName(), m.getAuthorRole(),
                        m.getContent(), m.getCreatedAt() == null ? null : m.getCreatedAt().toString()))
                .toList();
        return ClaimDto.Response.from(c, messages);
    }

    private void requireParticipant(Claim c, AuthenticatedUser me, Role role) {
        boolean autorise = switch (role) {
            case ADMIN, CHEF_DEPARTEMENT_PEDAGOGIQUE, CHEF_DEPARTEMENT_STAGE -> true;
            case ETUDIANT -> c.getStudentId().equals(lookup.student().id());
            case null, default -> false;
        };
        if (!autorise) {
            throw new ApiExceptions.ForbiddenException("Cette reclamation ne vous concerne pas");
        }
    }

    private Claim loadWithMessages(Long id) {
        return claims.findByIdWithMessages(id)
                .orElseThrow(() -> new ApiExceptions.NotFoundException("Reclamation", id));
    }
}
