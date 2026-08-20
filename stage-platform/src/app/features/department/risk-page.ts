import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core';

import { RiskAssessment } from '../../core/models/evaluation.models';
import { EvaluationService } from '../../core/services/evaluation.service';
import { InternshipService } from '../../core/services/internship.service';
import { EmptyState } from '../../shared/empty-state';
import { RiskGauge } from '../../shared/risk-gauge';
import { Spinner } from '../../shared/spinner';

/**
 * Suivi du risque sur l'ensemble des stages en cours.
 *
 * C'est la vue qui rend le volet apprentissage automatique utile : le
 * responsable voit d'un coup d'oeil quels stagiaires meritent un appel.
 */
@Component({
  selector: 'gs-risk-page',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [RiskGauge, EmptyState, Spinner],
  templateUrl: './risk-page.html',
})
export class RiskPage {
  private readonly internships = inject(InternshipService);
  private readonly evaluations = inject(EvaluationService);

  protected readonly chargement = signal(true);
  protected readonly evaluationsRisque = signal<readonly RiskAssessment[]>([]);
  protected readonly attendus = signal(0);

  /** Les risques eleves d'abord : c'est la seule information actionnable. */
  protected readonly triees = computed(() => {
    const poids: Record<string, number> = { HIGH: 0, MEDIUM: 1, LOW: 2, UNAVAILABLE: 3 };
    return [...this.evaluationsRisque()].sort(
      (a, b) => (poids[a.risk] ?? 9) - (poids[b.risk] ?? 9),
    );
  });

  protected readonly eleves = computed(
    () => this.evaluationsRisque().filter((r) => r.risk === 'HIGH').length,
  );

  constructor() {
    this.internships.forDepartment('IN_PROGRESS', 0, 100).subscribe({
      next: (page) => {
        this.attendus.set(page.content.length);
        this.chargement.set(false);
        page.content.forEach((i) =>
          this.evaluations.risk(i.id).subscribe({
            next: (r) => this.evaluationsRisque.update((liste) => [...liste, r]),
            error: () => undefined,
          }),
        );
      },
      error: () => this.chargement.set(false),
    });
  }
}
