import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core';

import { Company, UserService } from '../../core/services/user.service';
import { EmptyState } from '../../shared/empty-state';
import { Spinner } from '../../shared/spinner';

/**
 * Annuaire des entreprises partenaires.
 *
 * Sert a choisir ou postuler : l'etudiant y lit la presentation de
 * chacune avant de deposer sa demande, plutot que de decouvrir un nom
 * dans une liste deroulante. Le service des stages y retrouve ses
 * partenaires.
 *
 * Ni le courriel ni le telephone des encadrants n'y figurent : le
 * backend ne les sert pas a ce role, pour ne pas faire de cette page un
 * annuaire moissonnable.
 */
@Component({
  selector: 'gs-companies-page',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [EmptyState, Spinner],
  templateUrl: './companies-page.html',
})
export class CompaniesPage {
  private readonly users = inject(UserService);

  protected readonly chargement = signal(true);
  protected readonly entreprises = signal<readonly Company[]>([]);
  protected readonly recherche = signal('');

  /** Filtre sur le nom, l'adresse et la presentation. */
  protected readonly filtrees = computed(() => {
    const terme = this.recherche().trim().toLowerCase();
    if (terme.length === 0) {
      return this.entreprises();
    }
    return this.entreprises().filter((e) =>
      [e.name, e.address, e.description ?? ''].some((champ) =>
        champ.toLowerCase().includes(terme),
      ),
    );
  });

  constructor() {
    this.users.companies().subscribe({
      next: (page) => {
        this.entreprises.set(page.content);
        this.chargement.set(false);
      },
      error: () => {
        this.entreprises.set([]);
        this.chargement.set(false);
      },
    });
  }

  protected majRecherche(valeur: string): void {
    this.recherche.set(valeur);
  }
}
