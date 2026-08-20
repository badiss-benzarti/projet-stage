import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { Router } from '@angular/router';

import { RiskAssessment } from '../../core/models/evaluation.models';
import { Internship } from '../../core/models/internship.models';
import { EvaluationService } from '../../core/services/evaluation.service';
import { InternshipService } from '../../core/services/internship.service';
import { InternshipTable } from '../../shared/internship-table';
import { RiskGauge } from '../../shared/risk-gauge';
import { Spinner } from '../../shared/spinner';

/**
 * Tableau de bord de l'encadrant : ses stagiaires et, pour chacun, le
 * risque estime par le modele. C'est ici que le volet MLA devient utile.
 */
@Component({
  selector: 'gs-supervisor-dashboard',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [InternshipTable, RiskGauge, Spinner],
  templateUrl: './supervisor-dashboard.html',
})
export class SupervisorDashboard {
  private readonly internships = inject(InternshipService);
  private readonly evaluations = inject(EvaluationService);
  private readonly router = inject(Router);

  protected readonly chargement = signal(true);
  protected readonly stagiaires = signal<readonly Internship[]>([]);
  protected readonly risques = signal<readonly RiskAssessment[]>([]);

  constructor() {
    this.internships.forSupervision().subscribe({
      next: (page) => {
        this.stagiaires.set(page.content);
        this.chargement.set(false);
        // Un appel de prediction par stagiaire en cours : les dossiers
        // clos n'ont plus rien a signaler.
        page.content
          .filter((i) => i.status === 'IN_PROGRESS')
          .forEach((i) =>
            this.evaluations.risk(i.id).subscribe({
              next: (r) => this.risques.update((liste) => [...liste, r]),
            }),
          );
      },
      error: () => this.chargement.set(false),
    });
  }

  protected ouvrir(item: Internship): void {
    void this.router.navigate(['/encadrant/journaux'], { queryParams: { stage: item.id } });
  }
}
