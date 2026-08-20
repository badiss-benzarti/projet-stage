import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';

import { Evaluation } from '../../core/models/evaluation.models';
import { Internship } from '../../core/models/internship.models';
import { EvaluationService } from '../../core/services/evaluation.service';
import { InternshipService } from '../../core/services/internship.service';
import { EmptyState } from '../../shared/empty-state';
import { Spinner } from '../../shared/spinner';

/**
 * Note de stage vue par l'etudiant.
 *
 * Le detail du calcul est affiche : une note contestable doit etre
 * comprehensible, sinon la reclamation se fait a l'aveugle.
 */
@Component({
  selector: 'gs-grade-page',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [RouterLink, EmptyState, Spinner],
  templateUrl: './grade-page.html',
})
export class GradePage {
  private readonly internships = inject(InternshipService);
  private readonly evaluations = inject(EvaluationService);

  protected readonly chargement = signal(true);
  protected readonly dossier = signal<Internship | null>(null);
  protected readonly note = signal<Evaluation | null>(null);
  protected readonly message = signal<string | null>(null);

  constructor() {
    this.internships.mine(0, 1).subscribe({
      next: (page) => {
        const d = page.content.length > 0 ? page.content[0] : null;
        this.dossier.set(d);
        if (!d) {
          this.chargement.set(false);
          return;
        }
        this.evaluations.evaluation(d.id).subscribe({
          next: (e) => {
            this.note.set(e);
            this.chargement.set(false);
          },
          error: (err: { status?: number; error?: { message?: string } }) => {
            this.chargement.set(false);
            this.message.set(
              err.status === 403
                ? 'Votre évaluation n’est pas encore finalisée par l’encadrant.'
                : 'Aucune évaluation n’a encore été saisie pour votre stage.',
            );
          },
        });
      },
      error: () => this.chargement.set(false),
    });
  }

  /** Cles du detail, dans l'ordre du bareme. */
  protected criteres(e: Evaluation): readonly string[] {
    return Object.keys(e.breakdown);
  }
}
