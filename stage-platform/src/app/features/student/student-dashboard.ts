import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core';
import { SlicePipe } from '@angular/common';
import { RouterLink } from '@angular/router';

import {
  AvailableAction,
  estClose,
  Internship,
  InternshipStatus,
  STATUS_META,
} from '../../core/models/internship.models';
import { InternshipService } from '../../core/services/internship.service';
import { AuthService } from '../../core/services/auth.service';
import { EmptyState } from '../../shared/empty-state';
import { Spinner } from '../../shared/spinner';
import { StatusBadge } from '../../shared/status-badge';

/** Etapes affichees dans la frise. Les deux refus sortent du parcours. */
const PARCOURS: readonly InternshipStatus[] = [
  'DRAFT',
  'SUBMITTED',
  'UNDER_REVIEW',
  'APPROVED',
  'COMPANY_PENDING',
  'ACCEPTED',
  'IN_PROGRESS',
  'COMPLETED',
];

@Component({
  selector: 'gs-student-dashboard',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [RouterLink, SlicePipe, StatusBadge, EmptyState, Spinner],
  templateUrl: './student-dashboard.html',
})
export class StudentDashboard {
  private readonly internships = inject(InternshipService);
  private readonly auth = inject(AuthService);

  protected readonly prenom = computed(() => this.auth.currentUser()?.firstName ?? '');

  protected readonly chargement = signal(true);
  protected readonly erreur = signal<string | null>(null);
  protected readonly dossier = signal<Internship | null>(null);
  /** Les demandes autres que celle mise en avant. */
  protected readonly autres = signal<readonly Internship[]>([]);
  protected readonly actionEnCours = signal(false);
  protected readonly motif = signal('');

  protected readonly parcours = PARCOURS;
  protected readonly meta = STATUS_META;

  /** Position dans la frise, -1 si le dossier est sorti du parcours. */
  protected readonly etapeCourante = computed(() => {
    const d = this.dossier();
    return d ? PARCOURS.indexOf(d.status) : -1;
  });

  protected readonly estRefuse = computed(() => {
    const s = this.dossier()?.status;
    return s === 'REJECTED' || s === 'REFUSED';
  });

  constructor() {
    this.charger();
  }

  protected charger(): void {
    this.chargement.set(true);
    this.erreur.set(null);

    // L'etudiant peut avoir plusieurs demandes en parallele. On met en
    // avant celle qui l'engage - un stage accepte ou en cours - sinon la
    // plus recente encore ouverte ; les autres sont listees dessous.
    this.internships.mine(0, 50).subscribe({
      next: (page) => {
        const toutes = page.content;
        const principale =
          toutes.find((d) => d.status === 'IN_PROGRESS' || d.status === 'ACCEPTED') ??
          toutes.find((d) => !estClose(d.status)) ??
          toutes[0] ??
          null;

        this.dossier.set(principale);
        this.autres.set(toutes.filter((d) => d.id !== principale?.id));
        this.chargement.set(false);
      },
      error: (e: { status?: number; error?: { message?: string } }) => {
        this.chargement.set(false);
        this.erreur.set(
          e.status === 400
            ? (e.error?.message ?? 'Complétez d’abord votre profil étudiant.')
            : 'Impossible de charger votre dossier.',
        );
      },
    });
  }

  protected declencher(action: AvailableAction): void {
    const d = this.dossier();
    if (!d || this.actionEnCours()) {
      return;
    }
    if (action.requiresReason && this.motif().trim().length === 0) {
      this.erreur.set('Un motif est obligatoire pour cette action.');
      return;
    }

    this.actionEnCours.set(true);
    this.erreur.set(null);

    this.internships
      .transition(d.id, {
        target: action.target,
        comment: action.requiresReason ? this.motif().trim() : undefined,
      })
      .subscribe({
        next: (maj) => {
          this.dossier.set(maj);
          this.motif.set('');
          this.actionEnCours.set(false);
        },
        error: (e: { error?: { message?: string } }) => {
          this.actionEnCours.set(false);
          this.erreur.set(e.error?.message ?? 'Action impossible.');
        },
      });
  }

  protected majMotif(valeur: string): void {
    this.motif.set(valeur);
  }
}
