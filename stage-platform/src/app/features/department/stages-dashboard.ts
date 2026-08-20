import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';

import { InternshipStatus, STATUS_META } from '../../core/models/internship.models';
import { DocumentService } from '../../core/services/document.service';
import { InternshipService } from '../../core/services/internship.service';
import { Spinner } from '../../shared/spinner';

/** Tableau de bord du service des stages : volumes et files d'attente. */
@Component({
  selector: 'gs-stages-dashboard',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [RouterLink, Spinner],
  templateUrl: './stages-dashboard.html',
})
export class StagesDashboard {
  private readonly internships = inject(InternshipService);
  private readonly documents = inject(DocumentService);

  protected readonly chargement = signal(true);
  protected readonly stats = signal<Record<string, number | undefined>>({});
  protected readonly documentsEnAttente = signal(0);

  protected readonly meta = STATUS_META;

  /** Les etats a mettre en avant, dans l'ordre du parcours. */
  protected readonly ordre: readonly InternshipStatus[] = [
    'SUBMITTED',
    'UNDER_REVIEW',
    'APPROVED',
    'COMPANY_PENDING',
    'IN_PROGRESS',
    'COMPLETED',
  ];

  protected readonly total = computed(() =>
    Object.values(this.stats()).reduce<number>((somme, n) => somme + (n ?? 0), 0),
  );

  /** Ce qui demande une action immediate du service. */
  protected readonly aTraiter = computed(
    () => (this.stats()['SUBMITTED'] ?? 0) + (this.stats()['UNDER_REVIEW'] ?? 0),
  );

  constructor() {
    this.internships.statistics().subscribe({
      next: (s) => {
        this.stats.set(s);
        this.chargement.set(false);
      },
      error: () => this.chargement.set(false),
    });

    this.documents.pending().subscribe({
      next: (page) => this.documentsEnAttente.set(page.totalElements),
      error: () => this.documentsEnAttente.set(0),
    });
  }
}
