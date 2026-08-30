package com.gestionstages.user.service;

import com.gestionstages.user.dto.SupervisorDto;
import com.gestionstages.user.entity.Company;
import com.gestionstages.user.entity.Supervisor;
import com.gestionstages.user.exception.ApiExceptions;
import com.gestionstages.user.repository.SupervisorRepository;
import com.gestionstages.user.security.AuthenticatedUser;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class SupervisorService {

    private final SupervisorRepository supervisors;
    private final CompanyService companies;

    /**
     * L'entreprise declare ses encadrants.
     *
     * Le compte ENCADRANT doit avoir ete cree au prealable dans
     * l'auth-service : on reprend son userId ici pour faire le lien.
     */
    @Transactional
    public SupervisorDto.Response createForOwnCompany(AuthenticatedUser me, SupervisorDto.Request req) {
        Company company = companies.entityOfOwner(me);

        if (supervisors.existsByUserId(req.userId())) {
            throw new ApiExceptions.ProfileAlreadyExistsException("encadrant");
        }

        Supervisor s = Supervisor.builder()
                .userId(req.userId())
                .firstName(req.firstName())
                .lastName(req.lastName())
                .email(req.email().toLowerCase())
                .phone(req.phone())
                .position(req.position())
                .company(company)
                .build();

        return SupervisorDto.Response.from(supervisors.save(s));
    }

    @Transactional(readOnly = true)
    public SupervisorDto.Response findOwn(AuthenticatedUser me) {
        return supervisors.findByUserId(me.id())
                .map(SupervisorDto.Response::from)
                .orElseThrow(() -> new ApiExceptions.NotFoundException("Profil encadrant", me.email()));
    }

    @Transactional(readOnly = true)
    public SupervisorDto.Response findById(Long id) {
        return supervisors.findById(id)
                .map(SupervisorDto.Response::from)
                .orElseThrow(() -> new ApiExceptions.NotFoundException("Encadrant", id));
    }

    @Transactional(readOnly = true)
    public List<SupervisorDto.Response> findByCompany(Long companyId) {
        return supervisors.findByCompanyId(companyId).stream()
                .map(SupervisorDto.Response::from)
                .toList();
    }

    /**
     * Liste detaillee : reservee a l'entreprise proprietaire et a
     * l'administration. Sans ce controle, n'importe quelle entreprise
     * inscrite lirait les coordonnees des encadrants de ses concurrentes.
     */
    @Transactional(readOnly = true)
    public List<SupervisorDto.Response> findByCompanyForViewer(AuthenticatedUser me, Long companyId) {
        if ("ENTREPRISE".equals(me.role()) && !companies.entityOfOwner(me).getId().equals(companyId)) {
            throw new ApiExceptions.ForbiddenException(
                    "Vous ne pouvez consulter que les encadrants de votre entreprise");
        }
        return findByCompany(companyId);
    }

    /** Liste allegee, servie a l'etudiant qui depose sa demande de stage. */
    @Transactional(readOnly = true)
    public List<SupervisorDto.Option> findOptionsByCompany(Long companyId) {
        return supervisors.findByCompanyId(companyId).stream()
                .map(SupervisorDto.Option::from)
                .toList();
    }

    /** Une entreprise ne peut retirer qu'un encadrant qui lui appartient. */
    @Transactional
    public void deleteFromOwnCompany(AuthenticatedUser me, Long supervisorId) {
        Company company = companies.entityOfOwner(me);

        Supervisor s = supervisors.findById(supervisorId)
                .orElseThrow(() -> new ApiExceptions.NotFoundException("Encadrant", supervisorId));

        if (!s.getCompany().getId().equals(company.getId())) {
            throw new ApiExceptions.ForbiddenException(
                    "Cet encadrant appartient a une autre entreprise");
        }
        supervisors.delete(s);
    }
}
