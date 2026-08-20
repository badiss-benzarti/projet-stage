package com.gestionstages.user.service;

import com.gestionstages.user.dto.CompanyDto;
import com.gestionstages.user.entity.Company;
import com.gestionstages.user.exception.ApiExceptions;
import com.gestionstages.user.repository.CompanyRepository;
import com.gestionstages.user.security.AuthenticatedUser;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CompanyService {

    private final CompanyRepository companies;

    @Transactional
    public CompanyDto.Response createOwn(AuthenticatedUser me, CompanyDto.Request req) {
        if (companies.existsByUserId(me.id())) {
            throw new ApiExceptions.ProfileAlreadyExistsException("entreprise");
        }
        Company c = Company.builder()
                .userId(me.id())
                .name(req.name())
                .address(req.address())
                .phone(req.phone())
                .email(req.email().toLowerCase())
                .taxId(req.taxId())
                .build();
        return CompanyDto.Response.from(companies.save(c));
    }

    @Transactional(readOnly = true)
    public CompanyDto.Response findOwn(AuthenticatedUser me) {
        return companies.findByUserId(me.id())
                .map(CompanyDto.Response::from)
                .orElseThrow(() -> new ApiExceptions.NotFoundException("Profil entreprise", me.email()));
    }

    @Transactional(readOnly = true)
    public CompanyDto.Response findById(Long id) {
        return companies.findById(id)
                .map(CompanyDto.Response::from)
                .orElseThrow(() -> new ApiExceptions.NotFoundException("Entreprise", id));
    }

    @Transactional(readOnly = true)
    public Page<CompanyDto.Response> findAll(Pageable pageable) {
        return companies.findAll(pageable).map(CompanyDto.Response::from);
    }

    @Transactional
    public CompanyDto.Response updateOwn(AuthenticatedUser me, CompanyDto.Request req) {
        Company c = companies.findByUserId(me.id())
                .orElseThrow(() -> new ApiExceptions.NotFoundException("Profil entreprise", me.email()));

        c.setName(req.name());
        c.setAddress(req.address());
        c.setPhone(req.phone());
        c.setEmail(req.email().toLowerCase());
        c.setTaxId(req.taxId());

        return CompanyDto.Response.from(c);
    }

    /** Utilitaire interne : recupere l'entite de l'entreprise du porteur du jeton. */
    @Transactional(readOnly = true)
    public Company entityOfOwner(AuthenticatedUser me) {
        return companies.findByUserId(me.id())
                .orElseThrow(() -> new ApiExceptions.NotFoundException("Profil entreprise", me.email()));
    }
}
