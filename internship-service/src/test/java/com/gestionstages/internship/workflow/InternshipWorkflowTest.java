package com.gestionstages.internship.workflow;

import com.gestionstages.internship.enums.InternshipStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static com.gestionstages.internship.enums.InternshipStatus.*;
import static com.gestionstages.internship.enums.Role.*;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests de la machine a etats. Aucune base, aucun contexte Spring : la
 * table de transitions est une structure pure, donc testable en isolation.
 */
class InternshipWorkflowTest {

    private final InternshipWorkflow workflow = new InternshipWorkflow();

    @Test
    @DisplayName("le chemin nominal enchaine les huit etats attendus")
    void happyPathIsComplete() {
        InternshipStatus[] chemin = {
                DRAFT, SUBMITTED, UNDER_REVIEW, APPROVED,
                COMPANY_PENDING, ACCEPTED, IN_PROGRESS, COMPLETED};

        for (int i = 0; i < chemin.length - 1; i++) {
            assertThat(workflow.find(chemin[i], chemin[i + 1]))
                    .as("transition %s -> %s", chemin[i], chemin[i + 1])
                    .isPresent();
        }
    }

    @Test
    @DisplayName("un etudiant ne peut pas approuver sa propre demande")
    void studentCannotApprove() {
        Transition t = workflow.find(UNDER_REVIEW, APPROVED).orElseThrow();

        assertThat(t.isAllowedFor(ETUDIANT)).isFalse();
        assertThat(t.isAllowedFor(CHEF_DEPARTEMENT_STAGE)).isTrue();
    }

    @Test
    @DisplayName("une entreprise ne peut pas approuver a la place du departement")
    void companyCannotApprove() {
        Transition t = workflow.find(UNDER_REVIEW, APPROVED).orElseThrow();

        assertThat(t.isAllowedFor(ENTREPRISE)).isFalse();
    }

    @Test
    @DisplayName("les deux refus exigent un motif, les autres transitions non")
    void refusalsRequireAReason() {
        assertThat(workflow.find(UNDER_REVIEW, REJECTED).orElseThrow().requiresReason()).isTrue();
        assertThat(workflow.find(COMPANY_PENDING, REFUSED).orElseThrow().requiresReason()).isTrue();
        assertThat(workflow.find(UNDER_REVIEW, APPROVED).orElseThrow().requiresReason()).isFalse();
        assertThat(workflow.find(DRAFT, SUBMITTED).orElseThrow().requiresReason()).isFalse();
    }

    @Test
    @DisplayName("aucun saut d etape : un brouillon ne peut pas etre approuve directement")
    void noStateSkipping() {
        assertThat(workflow.find(DRAFT, APPROVED)).isEmpty();
        assertThat(workflow.find(DRAFT, COMPLETED)).isEmpty();
        assertThat(workflow.find(SUBMITTED, ACCEPTED)).isEmpty();
    }

    @Test
    @DisplayName("les etats finaux n admettent plus aucune transition")
    void terminalStatesAreClosed() {
        assertThat(workflow.from(REJECTED)).isEmpty();
        assertThat(workflow.from(REFUSED)).isEmpty();
        assertThat(workflow.from(COMPLETED)).isEmpty();

        assertThat(REJECTED.isTerminal()).isTrue();
        assertThat(REFUSED.isTerminal()).isTrue();
        assertThat(COMPLETED.isTerminal()).isTrue();
        assertThat(IN_PROGRESS.isTerminal()).isFalse();
    }

    @Test
    @DisplayName("ADMIN traverse toutes les transitions, c est le role de deblocage")
    void adminCanDoEverything() {
        for (InternshipStatus s : InternshipStatus.values()) {
            workflow.from(s).forEach(t ->
                    assertThat(t.isAllowedFor(ADMIN))
                            .as("ADMIN sur %s -> %s", s, t.to())
                            .isTrue());
        }
    }

    @Test
    @DisplayName("depuis UNDER_REVIEW le chef voit deux actions, l etudiant aucune")
    void availableActionsDependOnRole() {
        assertThat(workflow.availableFor(UNDER_REVIEW, CHEF_DEPARTEMENT_STAGE)).hasSize(2);
        assertThat(workflow.availableFor(UNDER_REVIEW, ETUDIANT)).isEmpty();
        assertThat(workflow.availableFor(COMPANY_PENDING, ENTREPRISE)).hasSize(2);
        assertThat(workflow.availableFor(COMPANY_PENDING, ENCADRANT)).isEmpty();
    }
}
