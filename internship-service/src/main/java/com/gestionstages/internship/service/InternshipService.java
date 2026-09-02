package com.gestionstages.internship.service;

import com.gestionstages.internship.client.UserClient;
import com.gestionstages.internship.client.UserLookup;
import com.gestionstages.internship.dto.InternshipDto;
import com.gestionstages.internship.entity.Internship;
import com.gestionstages.internship.entity.StatusHistory;
import com.gestionstages.internship.enums.InternshipStatus;
import com.gestionstages.internship.enums.Role;
import com.gestionstages.internship.event.InternshipEventPublisher;
import com.gestionstages.internship.exception.ApiExceptions;
import com.gestionstages.internship.repository.InternshipRepository;
import com.gestionstages.internship.security.AuthenticatedUser;
import com.gestionstages.internship.workflow.InternshipWorkflow;
import com.gestionstages.internship.workflow.Transition;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static com.gestionstages.internship.enums.InternshipStatus.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class InternshipService {

    private final InternshipRepository internships;
    private final InternshipWorkflow workflow;
    private final InternshipEventPublisher events;
    private final UserLookup lookup;

    // ==================================================================
    //  Creation et brouillon
    // ==================================================================

    /**
     * Cree un brouillon pour l'etudiant connecte.
     *
     * Plusieurs demandes en parallele sont permises : un etudiant
     * demarche plusieurs entreprises a la fois. L'ambiguite est levee a
     * l'acceptation, ou les demandes concurrentes sont classees sans
     * suite - voir classerLesAutresDemandes.
     *
     * Un stage deja demarre ferme en revanche la porte : on ne postule
     * plus ailleurs quand on a commence.
     */
    @Transactional
    public InternshipDto.Response create(AuthenticatedUser me, InternshipDto.Request req) {
        UserClient.StudentRef student = lookup.student();

        boolean stageEngage = internships
                .findByStudentIdAndStatusIn(student.id(), List.of(ACCEPTED, IN_PROGRESS))
                .stream().findAny().isPresent();

        if (stageEngage) {
            throw new ApiExceptions.BusinessRuleException(
                    "Vous avez deja un stage accepte : vous ne pouvez plus deposer de nouvelle demande");
        }

        validateDates(req);

        Internship i = Internship.builder()
                .studentId(student.id())
                .studentName(student.fullName())
                .studentEmail(student.email())
                .type(req.type())
                .title(req.title())
                .description(req.description())
                .academicYear(req.academicYear())
                .companyId(req.companyId())
                .companyName(req.companyName())
                .companyAddress(req.companyAddress())
                .companyEmail(req.companyEmail())
                .companyPhone(req.companyPhone())
                .contactName(req.contactName())
                .contactEmail(req.contactEmail())
                .contactPhone(req.contactPhone())
                .startDate(req.startDate())
                .endDate(req.endDate())
                .status(DRAFT)
                .build();

        applyRequestedSupervisor(i, req);

        internships.save(i);
        log.info("Brouillon de stage cree : {} (etudiant {})", i.getId(), student.id());

        return detail(i, me);
    }

    /** Un brouillon reste modifiable ; une fois soumis, le dossier est fige. */
    @Transactional
    public InternshipDto.Response updateDraft(AuthenticatedUser me, Long id, InternshipDto.Request req) {
        Internship i = load(id);
        requireOwner(i, me);

        if (i.getStatus() != DRAFT) {
            throw new ApiExceptions.InvalidTransitionException(
                    "Un dossier deja soumis ne peut plus etre modifie (etat " + i.getStatus() + ")");
        }

        validateDates(req);

        i.setType(req.type());
        i.setTitle(req.title());
        i.setDescription(req.description());
        i.setAcademicYear(req.academicYear());
        i.setCompanyId(req.companyId());
        i.setCompanyName(req.companyName());
        i.setCompanyAddress(req.companyAddress());
        i.setCompanyEmail(req.companyEmail());
        i.setCompanyPhone(req.companyPhone());
        i.setContactName(req.contactName());
        i.setContactEmail(req.contactEmail());
        i.setContactPhone(req.contactPhone());
        applyRequestedSupervisor(i, req);
        i.setStartDate(req.startDate());
        i.setEndDate(req.endDate());

        return detail(i, me);
    }

    /**
     * Enregistre l'encadrant pressenti par l'etudiant.
     *
     * L'identifiant vient du navigateur : rien ne garantit qu'il designe un
     * encadrant de l'entreprise choisie. On le confronte donc a la liste que
     * user-service tient pour cette entreprise, et on recopie le nom depuis
     * cette source plutot que depuis la requete.
     */
    private void applyRequestedSupervisor(Internship i, InternshipDto.Request req) {
        if (req.requestedSupervisorId() == null) {
            i.setRequestedSupervisorId(null);
            i.setRequestedSupervisorName(null);
            return;
        }
        if (req.companyId() == null) {
            throw new ApiExceptions.BusinessRuleException(
                    "Un encadrant ne peut etre designe que pour une entreprise inscrite sur la plateforme");
        }
        UserClient.SupervisorOption encadrant =
                resolveSupervisorOfCompany(req.companyId(), req.requestedSupervisorId());
        i.setRequestedSupervisorId(encadrant.id());
        i.setRequestedSupervisorName(encadrant.fullName());
    }

    /** Verifie qu'un encadrant appartient bien a l'entreprise d'accueil. */
    private UserClient.SupervisorOption resolveSupervisorOfCompany(Long companyId, Long supervisorId) {
        return lookup.supervisorOptions(companyId).stream()
                .filter(o -> supervisorId.equals(o.id()))
                .findFirst()
                .orElseThrow(() -> new ApiExceptions.BusinessRuleException(
                        "Cet encadrant n'est pas declare par l'entreprise d'accueil"));
    }

    @Transactional
    public void deleteDraft(AuthenticatedUser me, Long id) {
        Internship i = load(id);
        requireOwner(i, me);

        if (i.getStatus() != DRAFT) {
            throw new ApiExceptions.InvalidTransitionException("Seul un brouillon peut etre supprime");
        }
        internships.delete(i);
    }

    private boolean estVide(String valeur) {
        return valeur == null || valeur.isBlank();
    }

    private void validateDates(InternshipDto.Request req) {
        if (req.startDate() != null && req.endDate() != null
                && !req.endDate().isAfter(req.startDate())) {
            throw new ApiExceptions.BusinessRuleException(
                    "La date de fin doit etre posterieure a la date de debut");
        }
    }

    private Internship load(Long id) {
        return internships.findById(id)
                .orElseThrow(() -> new ApiExceptions.NotFoundException("Stage", id));
    }

    private void requireOwner(Internship i, AuthenticatedUser me) {
        if (Role.of(me.role()) == Role.ADMIN) return;
        if (!i.getStudentId().equals(lookup.student().id())) {
            throw new ApiExceptions.ForbiddenException("Ce dossier ne vous appartient pas");
        }
    }

    // ==================================================================
    //  Vue detaillee : etat + actions possibles + historique
    // ==================================================================

    private InternshipDto.Response detail(Internship i, AuthenticatedUser me) {
        Role role = Role.of(me.role());

        List<InternshipDto.AvailableAction> actions = workflow.availableFor(i.getStatus(), role).stream()
                .map(t -> new InternshipDto.AvailableAction(
                        t.to(), t.label(), t.hint(), t.requiresReason()))
                .toList();

        List<InternshipDto.HistoryEntry> history = i.getHistory().stream()
                .map(h -> new InternshipDto.HistoryEntry(
                        h.getFromStatus(), h.getToStatus(), h.getActorName(),
                        h.getActorRole(), h.getComment(),
                        h.getCreatedAt() == null ? null : h.getCreatedAt().toString()))
                .toList();

        return InternshipDto.Response.from(i, actions, history);
    }

    // ==================================================================
    //  Transition du workflow
    // ==================================================================

    /**
     * Applique une transition apres trois verifications : la transition
     * existe, le role la permet, le motif est fourni s'il est exige.
     * Toute la regle vient de InternshipWorkflow.
     */
    @Transactional
    public InternshipDto.Response transition(AuthenticatedUser me, Long id,
                                             InternshipDto.TransitionRequest req) {
        Internship i = load(id);
        Role role = Role.of(me.role());
        InternshipStatus from = i.getStatus();

        if (from.isTerminal()) {
            throw new ApiExceptions.InvalidTransitionException(
                    "Le dossier est dans un etat final (" + from + ")");
        }

        Transition t = workflow.find(from, req.target())
                .orElseThrow(() -> new ApiExceptions.InvalidTransitionException(
                        "Transition impossible : " + from + " -> " + req.target()));

        if (!t.isAllowedFor(role)) {
            throw new ApiExceptions.ForbiddenException(
                    "Le role " + role + " ne peut pas declencher : " + t.label());
        }

        if (t.requiresReason() && (req.comment() == null || req.comment().isBlank())) {
            throw new ApiExceptions.BusinessRuleException(
                    "Un motif est obligatoire pour : " + t.label());
        }

        applyBusinessEffects(me, i, req);

        i.setStatus(req.target());
        i.getHistory().add(StatusHistory.builder()
                .internship(i)
                .fromStatus(from)
                .toStatus(req.target())
                .actorId(me.id())
                .actorName(me.fullName())
                .actorRole(me.role())
                .comment(req.comment())
                .build());

        internships.save(i);
        log.info("Stage {} : {} -> {} par {} ({})", i.getId(), from, req.target(), me.email(), role);

        events.publish(i, from, me, req.comment());

        return detail(i, me);
    }

    /** Effets de bord propres a certaines transitions. */
    private void applyBusinessEffects(AuthenticatedUser me, Internship i,
                                      InternshipDto.TransitionRequest req) {
        switch (req.target()) {
            case SUBMITTED -> {
                // Deux chemins, deux jeux d'exigences.
                //
                // Entreprise inscrite : l'encadrant est un compte declare par
                // elle, choisi dans une liste. Entreprise sans compte : elle
                // n'a aucun encadrant a proposer, on se rabat sur le contact
                // saisi librement. Le cahier des charges impose de couvrir ce
                // second cas, un etudiant trouvant souvent son stage dans une
                // structure qui n'est pas referencee chez nous.
                if (estVide(i.getCompanyName())) {
                    throw new ApiExceptions.BusinessRuleException(
                            "Renseignez l'entreprise d'accueil avant de soumettre");
                }
                if (i.getCompanyId() != null) {
                    if (i.getRequestedSupervisorId() == null) {
                        throw new ApiExceptions.BusinessRuleException(
                                "Choisissez l'encadrant qui vous suivra, parmi ceux declares par l'entreprise");
                    }
                } else {
                    if (estVide(i.getCompanyEmail())) {
                        throw new ApiExceptions.BusinessRuleException(
                                "Renseignez l'email de l'entreprise d'accueil");
                    }
                    if (estVide(i.getContactName())) {
                        throw new ApiExceptions.BusinessRuleException(
                                "Renseignez le contact de l'encadrant en entreprise");
                    }
                }
                i.setSubmittedAt(Instant.now());
            }
            case REJECTED, REFUSED -> i.setRejectionReason(req.comment());
            case ACCEPTED -> {
                // Trois cas, dans cet ordre.
                //
                // 1. L'entreprise designe explicitement un encadrant : son
                //    choix prime, mais on verifie qu'il lui appartient - sans
                //    ce controle, elle pourrait ouvrir le dossier a un
                //    encadrant d'une autre societe.
                // 2. Elle ne designe personne alors que l'etudiant avait fait
                //    une proposition : accepter vaut confirmation.
                // 3. Entreprise sans compte : le service des stages enregistre
                //    la reponse, l'encadrant n'est connu que par son nom.
                if (req.supervisorId() != null && i.getCompanyId() != null) {
                    UserClient.SupervisorOption encadrant =
                            resolveSupervisorOfCompany(i.getCompanyId(), req.supervisorId());
                    i.setSupervisorId(encadrant.id());
                    i.setSupervisorName(encadrant.fullName());
                } else if (i.getRequestedSupervisorId() != null) {
                    i.setSupervisorId(i.getRequestedSupervisorId());
                    i.setSupervisorName(i.getRequestedSupervisorName());
                } else {
                    String nom = !estVide(req.supervisorName())
                            ? req.supervisorName()
                            : i.getContactName();
                    if (estVide(nom)) {
                        throw new ApiExceptions.BusinessRuleException(
                                "Designez un encadrant pour accepter le stagiaire");
                    }
                    i.setSupervisorName(nom);
                }

                classerLesAutresDemandes(me, i);
            }
            default -> { /* aucun effet de bord */ }
        }
    }

    /**
     * Une demande acceptee rend les autres sans objet.
     *
     * On les ferme au lieu de les supprimer : l'etudiant garde la trace
     * de ses demarches, et le motif explique la disparition plutot que
     * de laisser croire a une perte de donnees. Les dossiers deja
     * termines ou refuses ne sont pas touches, ils sont deja clos.
     */
    private void classerLesAutresDemandes(AuthenticatedUser me, Internship accepte) {
        List<Internship> autres = internships
                .findByStudentIdAndStatusNotIn(accepte.getStudentId(),
                        List.of(REJECTED, REFUSED, COMPLETED, ABANDONED))
                .stream()
                .filter(a -> !a.getId().equals(accepte.getId()))
                .toList();

        for (Internship a : autres) {
            InternshipStatus precedent = a.getStatus();
            a.setStatus(ABANDONED);
            a.setRejectionReason("Classee sans suite : votre stage chez "
                    + (accepte.getCompanyName() == null ? "une autre entreprise" : accepte.getCompanyName())
                    + " a ete accepte.");
            a.getHistory().add(StatusHistory.builder()
                    .internship(a)
                    .fromStatus(precedent)
                    .toStatus(ABANDONED)
                    .actorId(me.id())
                    .actorName(me.fullName())
                    .actorRole(me.role())
                    .comment("Une autre demande a abouti")
                    .build());
        }

        if (!autres.isEmpty()) {
            internships.saveAll(autres);
            log.info("{} demande(s) classee(s) sans suite pour l'etudiant {}",
                    autres.size(), accepte.getStudentId());
        }
    }

    // ==================================================================
    //  Consultation
    // ==================================================================

    @Transactional(readOnly = true)
    public InternshipDto.Response findById(AuthenticatedUser me, Long id) {
        Internship i = internships.findByIdWithHistory(id)
                .orElseThrow(() -> new ApiExceptions.NotFoundException("Stage", id));
        requireVisibility(i, me);
        return detail(i, me);
    }

    /** Un dossier n'est visible que de ses parties prenantes. */
    private void requireVisibility(Internship i, AuthenticatedUser me) {
        Role role = Role.of(me.role());
        boolean autorise = switch (role) {
            case ADMIN, CHEF_DEPARTEMENT_STAGE, CHEF_DEPARTEMENT_PEDAGOGIQUE -> true;
            case ETUDIANT   -> i.getStudentId().equals(lookup.student().id());
            case ENTREPRISE -> i.getCompanyId() != null
                    && i.getCompanyId().equals(lookup.company().id());
            case ENCADRANT  -> i.getSupervisorId() != null
                    && i.getSupervisorId().equals(lookup.supervisor().id());
            case null       -> false;
        };
        if (!autorise) {
            throw new ApiExceptions.ForbiddenException("Ce dossier ne vous concerne pas");
        }
    }

    @Transactional(readOnly = true)
    public Page<InternshipDto.Response> mine(AuthenticatedUser me, Pageable pageable) {
        return internships.findByStudentId(lookup.student().id(), pageable)
                .map(InternshipDto.Response::summary);
    }

    @Transactional(readOnly = true)
    public Page<InternshipDto.Response> forMyCompany(AuthenticatedUser me, InternshipStatus status,
                                                     Pageable pageable) {
        Long companyId = lookup.company().id();
        Page<Internship> page = (status == null)
                ? internships.findByCompanyId(companyId, pageable)
                : internships.findByCompanyIdAndStatus(companyId, status, pageable);
        return page.map(InternshipDto.Response::summary);
    }

    @Transactional(readOnly = true)
    public Page<InternshipDto.Response> forMySupervision(AuthenticatedUser me, Pageable pageable) {
        return internships.findBySupervisorId(lookup.supervisor().id(), pageable)
                .map(InternshipDto.Response::summary);
    }

    @Transactional(readOnly = true)
    public Page<InternshipDto.Response> forDepartment(InternshipStatus status, Pageable pageable) {
        // Un brouillon n'a pas ete envoye : il appartient encore a
        // l'etudiant seul. L'afficher au service des stages donnait a
        // instruire des dossiers que personne ne lui avait soumis.
        Page<Internship> page = (status == null)
                ? internships.findByStatusNot(DRAFT, pageable)
                : internships.findByStatus(status, pageable);
        return page.map(InternshipDto.Response::summary);
    }

    /** Repartition par etat, pour le tableau de bord des chefs de departement. */
    @Transactional(readOnly = true)
    public Map<String, Long> statistics() {
        Map<String, Long> stats = new LinkedHashMap<>();
        for (InternshipStatus s : InternshipStatus.values()) {
            stats.put(s.name(), 0L);
        }
        internships.countByStatus().forEach(row ->
                stats.put(((InternshipStatus) row[0]).name(), (Long) row[1]));
        return stats;
    }
}
