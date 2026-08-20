import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core';
import { ActivatedRoute } from '@angular/router';

import { Task, TASK_STATUS_META } from '../../core/models/evaluation.models';
import { Internship } from '../../core/models/internship.models';
import { EvaluationService } from '../../core/services/evaluation.service';
import { InternshipService } from '../../core/services/internship.service';
import { EmptyState } from '../../shared/empty-state';
import { Spinner } from '../../shared/spinner';

/**
 * Validation des taches du journal par l'encadrant.
 *
 * Le refus exige un motif : le champ apparait au moment de refuser, et
 * le bouton reste inactif tant qu'il est vide. La meme regle est
 * appliquee par le backend, qui renverrait 400.
 */
@Component({
  selector: 'gs-supervisor-journal-page',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [EmptyState, Spinner],
  templateUrl: './supervisor-journal-page.html',
})
export class SupervisorJournalPage {
  private readonly internships = inject(InternshipService);
  private readonly evaluations = inject(EvaluationService);
  private readonly route = inject(ActivatedRoute);

  protected readonly chargement = signal(true);
  protected readonly erreur = signal<string | null>(null);
  protected readonly dossiers = signal<readonly Internship[]>([]);
  protected readonly selection = signal<Internship | null>(null);
  protected readonly taches = signal<readonly Task[]>([]);

  /** Identifiant de la tache en cours de refus, et motif saisi. */
  protected readonly refusEnCours = signal<number | null>(null);
  protected readonly motif = signal('');
  protected readonly envoi = signal(false);

  protected readonly statutMeta = TASK_STATUS_META;

  protected readonly enAttente = computed(() =>
    this.taches().filter((t) => t.status === 'PENDING'),
  );
  protected readonly traitees = computed(() =>
    this.taches().filter((t) => t.status !== 'PENDING'),
  );

  constructor() {
    const demande = Number(this.route.snapshot.queryParamMap.get('stage'));

    this.internships.forSupervision().subscribe({
      next: (page) => {
        this.dossiers.set(page.content);
        const cible =
          page.content.find((i) => i.id === demande) ??
          page.content.find((i) => i.status === 'IN_PROGRESS') ??
          page.content[0] ??
          null;
        this.selection.set(cible);
        this.chargement.set(false);
        if (cible) {
          this.chargerTaches(cible.id);
        }
      },
      error: () => this.chargement.set(false),
    });
  }

  protected choisir(dossier: Internship): void {
    this.selection.set(dossier);
    this.annulerRefus();
    this.chargerTaches(dossier.id);
  }

  private chargerTaches(internshipId: number): void {
    this.evaluations.tasks(internshipId).subscribe({
      next: (page) => this.taches.set(page.content),
      error: () => this.taches.set([]),
    });
  }

  protected valider(tache: Task): void {
    this.decider(tache.id, 'VALIDATED');
  }

  protected commencerRefus(tache: Task): void {
    this.refusEnCours.set(tache.id);
    this.motif.set('');
    this.erreur.set(null);
  }

  protected annulerRefus(): void {
    this.refusEnCours.set(null);
    this.motif.set('');
  }

  protected confirmerRefus(tache: Task): void {
    if (this.motif().trim().length === 0) {
      this.erreur.set('Un motif est obligatoire pour refuser une tâche.');
      return;
    }
    this.decider(tache.id, 'REJECTED', this.motif().trim());
  }

  private decider(taskId: number, statut: 'VALIDATED' | 'REJECTED', motif?: string): void {
    if (this.envoi()) {
      return;
    }
    this.envoi.set(true);
    this.erreur.set(null);

    this.evaluations.decideTask(taskId, statut, motif).subscribe({
      next: (maj) => {
        this.taches.update((liste) => liste.map((t) => (t.id === maj.id ? maj : t)));
        this.envoi.set(false);
        this.annulerRefus();
      },
      error: (e: { error?: { message?: string } }) => {
        this.envoi.set(false);
        this.erreur.set(e.error?.message ?? 'Action impossible.');
      },
    });
  }

  protected majMotif(valeur: string): void {
    this.motif.set(valeur);
  }
}
