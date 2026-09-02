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
    @DisplayName("le chemin nominal enchaine les six etats attendus")
    void happyPathIsComplete() {
        InternshipStatus[] chemin = {
                DRAFT, SUBMITTED, COMPANY_PENDING, ACCEPTED, IN_PROGRESS, COMPLETED};

        for (int i = 0; i < chemin.length - 1; i++) {
            assertThat(workflow.find(chemin[i], chemin[i + 1]))
                    .as("transition %s -> %s", chemin[i], chemin[i + 1])
                    .isPresent();
        }
    }

    @Test
    @DisplayName("l etudiant envoie sa demande au service des stages, pas a l entreprise")
    void submissionGoesToTheDepartmentFirst() {
        Transition t = workflow.find(DRAFT, SUBMITTED).orElseThrow();

        assertThat(t.isAllowedFor(ETUDIANT)).isTrue();
        assertThat(t.isAllowedFor(CHEF_DEPARTEMENT_STAGE)).isFalse();

        // L'entreprise n'est saisie qu'apres l'accord de l'ecole.
        assertThat(workflow.find(DRAFT, COMPANY_PENDING)).isEmpty();
    }

    /**
     * Le service des stages tranche en une fois : accepter transmet a
     * l'entreprise, refuser clot le dossier. Rien entre les deux.
     */
    @Test
    @DisplayName("le departement n a que deux issues sur une demande envoyee")
    void departmentHasExactlyTwoOutcomes() {
        assertThat(workflow.availableFor(SUBMITTED, CHEF_DEPARTEMENT_STAGE)).hasSize(2);

        Transition accepter = workflow.find(SUBMITTED, COMPANY_PENDING).orElseThrow();
        Transition refuser = workflow.find(SUBMITTED, REJECTED).orElseThrow();

        assertThat(accepter.isAllowedFor(CHEF_DEPARTEMENT_STAGE)).isTrue();
        assertThat(accepter.requiresReason()).isFalse();
        assertThat(refuser.requiresReason()).isTrue();
    }

    @Test
    @DisplayName("ni l etudiant ni l entreprise ne decident a la place de l ecole")
    void nobodyElseInstructsTheFile() {
        Transition t = workflow.find(SUBMITTED, COMPANY_PENDING).orElseThrow();

        assertThat(t.isAllowedFor(ETUDIANT)).isFalse();
        assertThat(t.isAllowedFor(ENTREPRISE)).isFalse();
    }

    /**
     * Le service des stages instruit le dossier, mais ne repond pas a la
     * place de l'entreprise : la decision d'accueillir le stagiaire lui
     * appartient entierement.
     */
    @Test
    @DisplayName("le departement ne decide pas a la place de l entreprise")
    void departmentCannotAnswerForTheCompany() {
        assertThat(workflow.find(COMPANY_PENDING, ACCEPTED).orElseThrow()
                .isAllowedFor(CHEF_DEPARTEMENT_STAGE)).isFalse();
        assertThat(workflow.find(COMPANY_PENDING, REFUSED).orElseThrow()
                .isAllowedFor(CHEF_DEPARTEMENT_STAGE)).isFalse();
        assertThat(workflow.find(COMPANY_PENDING, ACCEPTED).orElseThrow()
                .isAllowedFor(ENTREPRISE)).isTrue();
    }

    @Test
    @DisplayName("les deux refus exigent un motif, les autres transitions non")
    void refusalsRequireAReason() {
        assertThat(workflow.find(SUBMITTED, REJECTED).orElseThrow().requiresReason()).isTrue();
        assertThat(workflow.find(COMPANY_PENDING, REFUSED).orElseThrow().requiresReason()).isTrue();
        assertThat(workflow.find(DRAFT, SUBMITTED).orElseThrow().requiresReason()).isFalse();
    }

    @Test
    @DisplayName("aucun saut d etape : un brouillon ne peut pas etre accepte directement")
    void noStateSkipping() {
        assertThat(workflow.find(DRAFT, ACCEPTED)).isEmpty();
        assertThat(workflow.find(DRAFT, COMPLETED)).isEmpty();
        assertThat(workflow.find(SUBMITTED, ACCEPTED)).isEmpty();
    }

    /**
     * L'instruction en trois temps a ete retiree. Ces etats subsistent
     * dans l'enumeration pour les dossiers historiques, mais plus aucune
     * transition n'y mene ni n'en part.
     */
    @Test
    @DisplayName("les anciens etats d instruction ne sont plus atteignables")
    void oldReviewStatesAreUnreachable() {
        assertThat(workflow.from(UNDER_REVIEW)).isEmpty();
        assertThat(workflow.from(APPROVED)).isEmpty();

        for (InternshipStatus s : InternshipStatus.values()) {
            assertThat(workflow.from(s).stream().map(Transition::to))
                    .as("plus aucune transition ne mene aux anciens etats")
                    .doesNotContain(UNDER_REVIEW, APPROVED);
        }
    }

    @Test
    @DisplayName("chaque action porte un libelle et une explication")
    void everyTransitionExplainsItself() {
        for (InternshipStatus s : InternshipStatus.values()) {
            workflow.from(s).forEach(t -> {
                assertThat(t.label()).as("libelle de %s -> %s", s, t.to()).isNotBlank();
                assertThat(t.hint()).as("explication de %s -> %s", s, t.to()).isNotBlank();
            });
        }
    }

    @Test
    @DisplayName("les etats finaux n admettent plus aucune transition")
    void terminalStatesAreClosed() {
        assertThat(workflow.from(REJECTED)).isEmpty();
        assertThat(workflow.from(REFUSED)).isEmpty();
        assertThat(workflow.from(COMPLETED)).isEmpty();
        assertThat(workflow.from(ABANDONED)).isEmpty();

        assertThat(REJECTED.isTerminal()).isTrue();
        assertThat(REFUSED.isTerminal()).isTrue();
        assertThat(COMPLETED.isTerminal()).isTrue();
        assertThat(ABANDONED.isTerminal()).isTrue();
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
    @DisplayName("les actions proposees dependent du role")
    void availableActionsDependOnRole() {
        assertThat(workflow.availableFor(SUBMITTED, CHEF_DEPARTEMENT_STAGE)).hasSize(2);
        assertThat(workflow.availableFor(SUBMITTED, ETUDIANT)).isEmpty();
        assertThat(workflow.availableFor(SUBMITTED, ENTREPRISE)).isEmpty();
        assertThat(workflow.availableFor(COMPANY_PENDING, ENTREPRISE)).hasSize(2);
        assertThat(workflow.availableFor(COMPANY_PENDING, ENCADRANT)).isEmpty();
        assertThat(workflow.availableFor(DRAFT, ETUDIANT)).hasSize(1);
    }
}
