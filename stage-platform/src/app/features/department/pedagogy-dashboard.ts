import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';

import { EvaluationService } from '../../core/services/evaluation.service';
import { InternshipService } from '../../core/services/internship.service';
import { Spinner } from '../../shared/spinner';

/** Tableau de bord du departement pedagogique. */
@Component({
  selector: 'gs-pedagogy-dashboard',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [RouterLink, Spinner],
  templateUrl: './pedagogy-dashboard.html',
})
export class PedagogyDashboard {
  private readonly evaluations = inject(EvaluationService);
  private readonly internships = inject(InternshipService);

  protected readonly chargement = signal(true);
  protected readonly notes = signal<Record<string, number | undefined>>({});
  protected readonly stages = signal<Record<string, number | undefined>>({});
  protected readonly reclamationsOuvertes = signal(0);

  protected readonly enCours = computed(() => this.stages()['IN_PROGRESS'] ?? 0);
  protected readonly termines = computed(() => this.stages()['COMPLETED'] ?? 0);

  /** Combien de stages termines n'ont pas encore de note validee. */
  protected readonly notesManquantes = computed(() =>
    Math.max(0, this.termines() - (this.notes()['count'] ?? 0)),
  );

  constructor() {
    this.evaluations.statistics().subscribe({
      next: (s) => {
        this.notes.set(s);
        this.chargement.set(false);
      },
      error: () => this.chargement.set(false),
    });

    this.internships.statistics().subscribe({
      next: (s) => this.stages.set(s),
      error: () => this.stages.set({}),
    });

    this.evaluations.departmentClaims().subscribe({
      next: (page) => this.reclamationsOuvertes.set(page.totalElements),
      error: () => this.reclamationsOuvertes.set(0),
    });
  }
}
