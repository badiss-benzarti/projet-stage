import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { DatePipe } from '@angular/common';

import {
  DocumentRequest,
  REQUEST_TYPE_LABELS,
} from '../../core/models/internship.models';
import { DocumentService } from '../../core/services/document.service';
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
  private readonly documents = inject(DocumentService);

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

  /**
   * Accepter, produire le PDF, puis le rattacher a la demande.
   *
   * Les trois etapes s'enchainent ici parce qu'elles vivent dans deux
   * services distincts : internship-service instruit la demande,
   * document-service edite le fichier. Si l'edition echoue, la demande
   * reste acceptee mais sans document - c'est precisement l'etat que
   * markIssued sert a distinguer, et le message le dit.
   */
  protected editer(demande: DocumentRequest): void {
    if (this.envoi()) {
      return;
    }
    this.envoi.set(true);
    this.erreur.set(null);

    this.internships.decideRequest(demande.id, 'ISSUED').subscribe({
      next: () => {
        if (demande.type !== 'LETTRE_AFFECTATION' || demande.internshipId === null) {
          this.retirer(demande.id);
          return;
        }
        this.documents.genererLettreAffectation(demande.internshipId).subscribe({
          next: (doc) =>
            this.internships.markRequestIssued(demande.id, doc.id).subscribe({
              next: () => this.retirer(demande.id),
              error: () => this.retirer(demande.id),
            }),
          error: (e: { error?: { message?: string } }) => {
            this.envoi.set(false);
            this.erreur.set(
              'Demande acceptée, mais le document n’a pas pu être édité : ' +
                (e.error?.message ?? 'erreur inconnue'),
            );
          },
        });
      },
      error: (e: { error?: { message?: string } }) => {
        this.envoi.set(false);
        this.erreur.set(e.error?.message ?? 'Décision impossible.');
      },
    });
  }

  private retirer(id: number): void {
    this.demandes.update((liste) => liste.filter((d) => d.id !== id));
    this.envoi.set(false);
    this.annulerRefus();
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
