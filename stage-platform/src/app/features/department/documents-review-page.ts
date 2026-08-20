import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { DatePipe } from '@angular/common';

import {
  DOCUMENT_TYPE_LABELS,
  DocumentService,
  StageDocument,
} from '../../core/services/document.service';
import { telechargerBlob } from '../../shared/download';
import { EmptyState } from '../../shared/empty-state';
import { Spinner } from '../../shared/spinner';

/**
 * Validation des documents deposes.
 *
 * Le cahier des charges exige l'acceptation ou le refus AVEC MOTIF : le
 * champ s'ouvre au moment du refus, et le backend rejetterait un refus
 * sans motif.
 */
@Component({
  selector: 'gs-documents-review-page',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [DatePipe, EmptyState, Spinner],
  templateUrl: './documents-review-page.html',
})
export class DocumentsReviewPage {
  private readonly documents = inject(DocumentService);

  protected readonly chargement = signal(true);
  protected readonly envoi = signal(false);
  protected readonly erreur = signal<string | null>(null);

  protected readonly liste = signal<readonly StageDocument[]>([]);
  protected readonly refusEnCours = signal<number | null>(null);
  protected readonly motif = signal('');

  protected readonly typeLabels = DOCUMENT_TYPE_LABELS;

  constructor() {
    this.charger();
  }

  private charger(): void {
    this.chargement.set(true);
    this.documents.pending().subscribe({
      next: (page) => {
        this.liste.set(page.content);
        this.chargement.set(false);
      },
      error: () => this.chargement.set(false),
    });
  }

  protected telecharger(doc: StageDocument): void {
    this.documents.download(doc.id).subscribe({
      next: (blob) => telechargerBlob(blob, doc.originalName),
      error: () => this.erreur.set('Téléchargement impossible.'),
    });
  }

  protected valider(doc: StageDocument): void {
    this.decider(doc.id, 'APPROVED');
  }

  protected commencerRefus(doc: StageDocument): void {
    this.refusEnCours.set(doc.id);
    this.motif.set('');
    this.erreur.set(null);
  }

  protected annulerRefus(): void {
    this.refusEnCours.set(null);
    this.motif.set('');
  }

  protected confirmerRefus(doc: StageDocument): void {
    if (this.motif().trim().length === 0) {
      this.erreur.set('Un motif est obligatoire pour refuser un document.');
      return;
    }
    this.decider(doc.id, 'REJECTED', this.motif().trim());
  }

  private decider(id: number, statut: 'APPROVED' | 'REJECTED', motif?: string): void {
    if (this.envoi()) {
      return;
    }
    this.envoi.set(true);
    this.erreur.set(null);

    this.documents.decide(id, statut, motif).subscribe({
      next: () => {
        // Le document quitte la file d'attente une fois tranche.
        this.liste.update((l) => l.filter((d) => d.id !== id));
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
