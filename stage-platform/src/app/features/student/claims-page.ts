import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';

import { Claim, ClaimType } from '../../core/models/evaluation.models';
import { Internship } from '../../core/models/internship.models';
import { EvaluationService } from '../../core/services/evaluation.service';
import { InternshipService } from '../../core/services/internship.service';
import { ClaimThread } from '../../shared/claim-thread';
import { EmptyState } from '../../shared/empty-state';
import { Spinner } from '../../shared/spinner';

/** Reclamations de l'etudiant : depot et relances. */
@Component({
  selector: 'gs-claims-page',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [ReactiveFormsModule, ClaimThread, EmptyState, Spinner],
  templateUrl: './claims-page.html',
})
export class ClaimsPage {
  private readonly fb = inject(FormBuilder);
  private readonly evaluations = inject(EvaluationService);
  private readonly internships = inject(InternshipService);

  protected readonly chargement = signal(true);
  protected readonly envoi = signal(false);
  protected readonly erreur = signal<string | null>(null);
  protected readonly formulaireOuvert = signal(false);

  protected readonly dossier = signal<Internship | null>(null);
  protected readonly reclamations = signal<readonly Claim[]>([]);

  protected readonly types: readonly { valeur: ClaimType; libelle: string }[] = [
    { valeur: 'NOTE', libelle: 'Contestation de note' },
    { valeur: 'TACHE', libelle: 'Validation d’une tâche' },
    { valeur: 'AUTRE', libelle: 'Autre motif' },
  ];

  protected readonly form = this.fb.nonNullable.group({
    type: ['NOTE' as ClaimType, Validators.required],
    subject: ['', [Validators.required, Validators.maxLength(200)]],
    message: ['', Validators.required],
  });

  constructor() {
    this.internships.mine(0, 1).subscribe({
      next: (page) => this.dossier.set(page.content[0] ?? null),
    });
    this.recharger();
  }

  private recharger(): void {
    this.evaluations.myClaims().subscribe({
      next: (page) => {
        // La liste ne porte pas les messages : on recharge chaque
        // reclamation pour afficher son fil complet.
        this.reclamations.set([]);
        page.content.forEach((c) =>
          this.evaluations.claim(c.id).subscribe({
            next: (complet) => this.reclamations.update((liste) => [...liste, complet]),
          }),
        );
        this.chargement.set(false);
      },
      error: () => this.chargement.set(false),
    });
  }

  protected basculerFormulaire(): void {
    this.formulaireOuvert.update((v) => !v);
    this.erreur.set(null);
  }

  protected deposer(): void {
    const d = this.dossier();
    if (!d) {
      this.erreur.set('Aucun dossier de stage : impossible de déposer une réclamation.');
      return;
    }
    if (this.form.invalid || this.envoi()) {
      this.form.markAllAsTouched();
      return;
    }

    const v = this.form.getRawValue();
    this.envoi.set(true);
    this.erreur.set(null);

    this.evaluations.openClaim(d.id, v.type, v.subject.trim(), v.message.trim()).subscribe({
      next: (c) => {
        this.reclamations.update((liste) => [c, ...liste]);
        this.form.reset({ type: 'NOTE', subject: '', message: '' });
        this.formulaireOuvert.set(false);
        this.envoi.set(false);
      },
      error: (e: { error?: { message?: string } }) => {
        this.envoi.set(false);
        this.erreur.set(e.error?.message ?? 'Dépôt impossible.');
      },
    });
  }

  protected repondre(claim: Claim, contenu: string): void {
    this.envoi.set(true);
    this.evaluations.replyToClaim(claim.id, contenu).subscribe({
      next: (maj) => {
        this.reclamations.update((liste) => liste.map((c) => (c.id === maj.id ? maj : c)));
        this.envoi.set(false);
      },
      error: (e: { error?: { message?: string } }) => {
        this.envoi.set(false);
        this.erreur.set(e.error?.message ?? 'Envoi impossible.');
      },
    });
  }
}
