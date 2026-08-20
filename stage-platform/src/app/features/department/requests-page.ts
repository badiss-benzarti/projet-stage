import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { DatePipe } from '@angular/common';

import {
  DocumentRequest,
  REQUEST_TYPE_LABELS,
} from '../../core/models/internship.models';
import { InternshipService } from '../../core/services/internship.service';
import { EmptyState } from '../../shared/empty-state';
import { Spinner } from '../../shared/spinner';

/**
 * Instruction des demandes de convention et de lettre d'affectation.
 *
 * Distinct du depot de documents : ici on edite un document, la-bas on
 * valide un fichier signe rapporte par l'etudiant.
 */
@Component({
  selector: 'gs-requests-page',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [DatePipe, EmptyState, Spinner],
  templateUrl: './requests-page.html',
})
export class RequestsPage {
  private readonly internships = inject(InternshipService);

  protected readonly chargement = signal(true);
  protected readonly envoi = signal(false);
  protected readonly erreur = signal<string | null>(null);

  protected readonly demandes = signal<readonly DocumentRequest[]>([]);
  protected readonly refusEnCours = signal<number | null>(null);
  protected readonly motif = signal('');

  protected readonly typeLabels = REQUEST_TYPE_LABELS;

  constructor() {
    this.internships.pendingRequests().subscribe({
      next: (page) => {
        this.demandes.set(page.content);
        this.chargement.set(false);
      },
      error: () => this.chargement.set(false),
    });
  }

  protected editer(demande: DocumentRequest): void {
    this.decider(demande.id, 'ISSUED');
  }

  protected commencerRefus(demande: DocumentRequest): void {
    this.refusEnCours.set(demande.id);
    this.motif.set('');
    this.erreur.set(null);
  }

  protected annulerRefus(): void {
    this.refusEnCours.set(null);
    this.motif.set('');
  }

  protected confirmerRefus(demande: DocumentRequest): void {
    if (this.motif().trim().length === 0) {
      this.erreur.set('Un motif est obligatoire pour refuser une demande.');
      return;
    }
    this.decider(demande.id, 'REJECTED', this.motif().trim());
  }

  private decider(id: number, statut: 'ISSUED' | 'REJECTED', motif?: string): void {
    if (this.envoi()) {
      return;
    }
    this.envoi.set(true);
    this.erreur.set(null);

    this.internships.decideRequest(id, statut, motif).subscribe({
      next: () => {
        this.demandes.update((liste) => liste.filter((d) => d.id !== id));
        this.envoi.set(false);
        this.annulerRefus();
      },
      error: (e: { error?: { message?: string } }) => {
        this.envoi.set(false);
        this.erreur.set(e.error?.message ?? 'Décision impossible.');
      },
    });
  }

  protected majMotif(valeur: string): void {
    this.motif.set(valeur);
  }
}
