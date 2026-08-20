import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';

import { Evaluation } from '../../core/models/evaluation.models';
import { Internship } from '../../core/models/internship.models';
import { EvaluationService } from '../../core/services/evaluation.service';
import { InternshipService } from '../../core/services/internship.service';
import { telechargerBlob } from '../../shared/download';
import { EmptyState } from '../../shared/empty-state';
import { Spinner } from '../../shared/spinner';

/**
 * Notes de stage vues par le departement pedagogique, et export XLSX
 * exige par le cahier des charges.
 */
@Component({
  selector: 'gs-grades-page',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [EmptyState, Spinner],
  templateUrl: './grades-page.html',
})
export class GradesPage {
  private readonly evaluations = inject(EvaluationService);
  private readonly internships = inject(InternshipService);

  protected readonly chargement = signal(true);
  protected readonly export = signal(false);
  protected readonly erreur = signal<string | null>(null);

  protected readonly notes = signal<readonly Evaluation[]>([]);
  protected readonly stats = signal<Record<string, number | undefined>>({});

  constructor() {
    this.evaluations.statistics().subscribe({
      next: (s) => this.stats.set(s),
      error: () => this.stats.set({}),
    });

    // Le backend n'expose pas de liste des evaluations : on part des
    // stages termines ou en cours et on interroge leur grille.
    this.internships.forDepartment(undefined, 0, 100).subscribe({
      next: (page) => {
        const eligibles = page.content.filter(
          (i: Internship) => i.status === 'IN_PROGRESS' || i.status === 'COMPLETED',
        );
        this.chargement.set(false);
        eligibles.forEach((i) =>
          this.evaluations.evaluation(i.id).subscribe({
            next: (e) => {
              if (e.status === 'SUBMITTED') {
                this.notes.update((liste) => [...liste, e]);
              }
            },
            // 404 : pas encore de grille pour ce stage, rien a afficher.
            error: () => undefined,
          }),
        );
      },
      error: () => this.chargement.set(false),
    });
  }

  protected exporter(): void {
    this.export.set(true);
    this.erreur.set(null);

    this.evaluations.notesXlsx().subscribe({
      next: (blob) => {
        const jour = new Date().toISOString().slice(0, 10);
        telechargerBlob(blob, `notes-stages-${jour}.xlsx`);
        this.export.set(false);
      },
      error: () => {
        this.export.set(false);
        this.erreur.set('Export impossible.');
      },
    });
  }
}
