import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';

import { Internship } from '../../core/models/internship.models';
import { InternshipService } from '../../core/services/internship.service';
import { Company, UserService } from '../../core/services/user.service';
import { Spinner } from '../../shared/spinner';

/** Tableau de bord de l'entreprise d'accueil. */
@Component({
  selector: 'gs-company-dashboard',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [RouterLink, Spinner],
  templateUrl: './company-dashboard.html',
})
export class CompanyDashboard {
  private readonly internships = inject(InternshipService);
  private readonly users = inject(UserService);

  protected readonly chargement = signal(true);
  protected readonly erreur = signal<string | null>(null);
  protected readonly entreprise = signal<Company | null>(null);
  protected readonly dossiers = signal<readonly Internship[]>([]);

  protected readonly enAttente = computed(
    () => this.dossiers().filter((d) => d.status === 'COMPANY_PENDING').length,
  );
  protected readonly enCours = computed(
    () => this.dossiers().filter((d) => d.status === 'IN_PROGRESS').length,
  );
  protected readonly termines = computed(
    () => this.dossiers().filter((d) => d.status === 'COMPLETED').length,
  );

  constructor() {
    this.users.myCompany().subscribe({
      next: (c) => this.entreprise.set(c),
      error: () =>
        this.erreur.set(
          'Complétez d’abord le profil de votre entreprise pour accéder à vos stagiaires.',
        ),
    });

    this.internships.forCompany(undefined, 0, 100).subscribe({
      next: (page) => {
        this.dossiers.set(page.content);
        this.chargement.set(false);
      },
      error: () => this.chargement.set(false),
    });
  }
}
