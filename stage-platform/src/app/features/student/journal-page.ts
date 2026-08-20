import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';

import {
  Task,
  TaskSummary,
  TASK_STATUS_META,
} from '../../core/models/evaluation.models';
import { Internship } from '../../core/models/internship.models';
import { EvaluationService } from '../../core/services/evaluation.service';
import { InternshipService } from '../../core/services/internship.service';
import { EmptyState } from '../../shared/empty-state';
import { Spinner } from '../../shared/spinner';

/**
 * Journal de stage cote etudiant.
 *
 * Le journal n'est ouvert que pendant le stage : le backend refuse toute
 * saisie hors de l'etat IN_PROGRESS, on le signale ici plutot que de
 * laisser l'utilisateur decouvrir l'erreur en validant.
 */
@Component({
  selector: 'gs-journal-page',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [ReactiveFormsModule, EmptyState, Spinner],
  templateUrl: './journal-page.html',
})
export class JournalPage {
  private readonly fb = inject(FormBuilder);
  private readonly evaluations = inject(EvaluationService);
  private readonly internships = inject(InternshipService);

  protected readonly chargement = signal(true);
  protected readonly envoi = signal(false);
  protected readonly erreur = signal<string | null>(null);

  protected readonly dossier = signal<Internship | null>(null);
  protected readonly taches = signal<readonly Task[]>([]);
  protected readonly synthese = signal<TaskSummary | null>(null);

  protected readonly statutMeta = TASK_STATUS_META;
  protected readonly aujourdhui = new Date().toISOString().slice(0, 10);

  protected readonly journalOuvert = computed(() => this.dossier()?.status === 'IN_PROGRESS');

  /** Tache en cours de correction apres un refus, sinon null. */
  protected readonly enEdition = signal<number | null>(null);

  protected readonly form = this.fb.nonNullable.group({
    taskDate: [this.aujourdhui, Validators.required],
    title: ['', [Validators.required, Validators.maxLength(200)]],
    description: [''],
    hours: [7, [Validators.required, Validators.min(0.5), Validators.max(12)]],
  });

  constructor() {
    this.charger();
  }

  private charger(): void {
    this.internships.mine(0, 1).subscribe({
      next: (page) => {
        const d = page.content.length > 0 ? page.content[0] : null;
        this.dossier.set(d);
        if (d) {
          this.rafraichir(d.id);
        } else {
          this.chargement.set(false);
        }
      },
      error: () => {
        this.chargement.set(false);
        this.erreur.set('Impossible de charger votre dossier.');
      },
    });
  }

  private rafraichir(internshipId: number): void {
    this.evaluations.tasks(internshipId).subscribe({
      next: (page) => {
        this.taches.set(page.content);
        this.chargement.set(false);
      },
      error: () => this.chargement.set(false),
    });
    this.evaluations.taskSummary(internshipId).subscribe({
      next: (s) => this.synthese.set(s),
    });
  }

  protected soumettre(): void {
    const d = this.dossier();
    if (!d || this.form.invalid || this.envoi()) {
      this.form.markAllAsTouched();
      return;
    }

    const valeurs = this.form.getRawValue();
    const charge = {
      taskDate: valeurs.taskDate,
      title: valeurs.title.trim(),
      description: valeurs.description.trim() || null,
      hours: valeurs.hours,
    };

    this.envoi.set(true);
    this.erreur.set(null);

    const idEnEdition = this.enEdition();
    const appel = idEnEdition
      ? this.evaluations.updateTask(idEnEdition, charge)
      : this.evaluations.addTask(d.id, charge);

    appel.subscribe({
      next: () => {
        this.envoi.set(false);
        this.enEdition.set(null);
        this.form.reset({ taskDate: this.aujourdhui, title: '', description: '', hours: 7 });
        this.rafraichir(d.id);
      },
      error: (e: { error?: { message?: string; champs?: Record<string, string> } }) => {
        this.envoi.set(false);
        const champs = e.error?.champs;
        this.erreur.set(
          champs ? Object.values(champs).join(' · ') : (e.error?.message ?? 'Saisie refusée.'),
        );
      },
    });
  }

  protected corriger(tache: Task): void {
    this.enEdition.set(tache.id);
    this.form.setValue({
      taskDate: tache.taskDate,
      title: tache.title,
      description: tache.description ?? '',
      hours: tache.hours,
    });
    window.scrollTo({ top: 0, behavior: 'smooth' });
  }

  protected annulerEdition(): void {
    this.enEdition.set(null);
    this.form.reset({ taskDate: this.aujourdhui, title: '', description: '', hours: 7 });
  }

  protected supprimer(tache: Task): void {
    const d = this.dossier();
    if (!d) {
      return;
    }
    this.evaluations.deleteTask(tache.id).subscribe({
      next: () => this.rafraichir(d.id),
      error: (e: { error?: { message?: string } }) =>
        this.erreur.set(e.error?.message ?? 'Suppression impossible.'),
    });
  }

  /** Telechargement du journal en PDF, genere par evaluation-service. */
  protected telechargerPdf(): void {
    const d = this.dossier();
    if (!d) {
      return;
    }
    this.evaluations.journalPdf(d.id).subscribe({
      next: (blob) => {
        const url = URL.createObjectURL(blob);
        const lien = document.createElement('a');
        lien.href = url;
        lien.download = `journal-stage-${d.id}.pdf`;
        lien.click();
        URL.revokeObjectURL(url);
      },
      error: () => this.erreur.set('Génération du PDF impossible.'),
    });
  }
}
