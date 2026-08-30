import { ChangeDetectionStrategy, Component } from '@angular/core';
import { Router, RouterLink } from '@angular/router';

import { Role } from '../../core/models/auth.models';
import { SessionNotice } from '../../shared/session-notice';

interface TypeCompte {
  readonly role: Role;
  readonly libelle: string;
  readonly description: string;
  /** Faux quand le compte est cree par un tiers, pas en libre-service. */
  readonly ouvert: boolean;
  readonly note?: string;
}

/**
 * Choix du type de compte.
 *
 * Les quatre profils sont montres, mais deux seulement sont ouverts a
 * l'inscription libre. Afficher les autres en grise, avec la raison,
 * evite qu'un encadrant cherche vainement comment s'inscrire.
 */
@Component({
  selector: 'gs-account-type-page',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [RouterLink, SessionNotice],
  templateUrl: './account-type-page.html',
})
export class AccountTypePage {
  protected readonly types: readonly TypeCompte[] = [
    {
      role: 'ETUDIANT',
      libelle: 'Étudiant',
      description: 'Déposer une demande, tenir mon journal, consulter ma note.',
      ouvert: true,
    },
    {
      role: 'ENTREPRISE',
      libelle: 'Entreprise',
      description: 'Accueillir des stagiaires et déclarer mes encadrants.',
      ouvert: true,
    },
    {
      role: 'ENCADRANT',
      libelle: 'Encadrant',
      description: 'Valider le journal et remplir la grille d’évaluation.',
      ouvert: false,
      note: 'Votre entreprise vous déclare et crée votre compte.',
    },
    {
      role: 'CHEF_DEPARTEMENT_STAGE',
      libelle: 'Responsable de département',
      description: 'Instruire les demandes, valider les documents, suivre les notes.',
      ouvert: false,
      note: 'Compte créé par l’administration de l’école.',
    },
  ];
}
