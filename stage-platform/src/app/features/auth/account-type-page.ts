import { ChangeDetectionStrategy, Component } from '@angular/core';
import { RouterLink } from '@angular/router';

import { Role } from '../../core/models/auth.models';
import { SessionNotice } from '../../shared/session-notice';

interface TypeCompte {
  readonly role: Role;
  readonly libelle: string;
  readonly description: string;
}

/**
 * Choix du type de compte.
 *
 * Seuls les deux profils ouverts a l'inscription libre sont proposes.
 * Les encadrants sont declares par leur entreprise et les responsables
 * de departement par l'administration : les montrer grises n'offrait
 * aucune action, seulement deux cases mortes a cote des vraies.
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
    },
    {
      role: 'ENTREPRISE',
      libelle: 'Entreprise',
      description: 'Accueillir des stagiaires et déclarer mes encadrants.',
    },
  ];
}
