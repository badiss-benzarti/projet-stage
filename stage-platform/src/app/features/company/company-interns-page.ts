import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';

import { Internship } from '../../core/models/internship.models';
import { InternshipService } from '../../core/services/internship.service';
import { InternshipTable } from '../../shared/internship-table';
import { Spinner } from '../../shared/spinner';
import { StatusBadge } from '../../shared/status-badge';

/** Tous les stages accueillis par l'entreprise. */
@Component({
  selector: 'gs-company-interns-page',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [InternshipTable, StatusBadge, Spinner],
  templateUrl: './company-interns-page.html',
})
export class CompanyInternsPage {
  private readonly internships = inject(InternshipService);

  protected readonly chargement = signal(true);
  protected readonly dossiers = signal<readonly Internship[]>([]);
  protected readonly selection = signal<Internship | null>(null);

  constructor() {
    this.internships.forCompany(undefined, 0, 100).subscribe({
      next: (page) => {
        this.dossiers.set(page.content);
        this.chargement.set(false);
      },
      error: () => this.chargement.set(false),
    });
  }

  protected ouvrir(dossier: Internship): void {
    this.internships.byId(dossier.id).subscribe({
      next: (complet) => this.selection.set(complet),
    });
  }

  protected fermer(): void {
    this.selection.set(null);
  }
}
