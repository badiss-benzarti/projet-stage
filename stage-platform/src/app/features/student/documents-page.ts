import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';

import { Internship } from '../../core/models/internship.models';
import {
  DOCUMENT_STATUS_META,
  DOCUMENT_TYPE_LABELS,
  DocumentService,
  DocumentType,
  StageDocument,
} from '../../core/services/document.service';
import { InternshipService } from '../../core/services/internship.service';
import { telechargerBlob } from '../../shared/download';
import { EmptyState } from '../../shared/empty-state';
import { Spinner } from '../../shared/spinner';

/** Depot et suivi des documents de stage cote etudiant. */
@Component({
  selector: 'gs-documents-page',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [EmptyState, Spinner],
  templateUrl: './documents-page.html',
})
export class DocumentsPage {
  private readonly documents = inject(DocumentService);
  private readonly internships = inject(InternshipService);

  protected readonly chargement = signal(true);
  protected readonly envoi = signal(false);
  protected readonly erreur = signal<string | null>(null);

  protected readonly dossier = signal<Internship | null>(null);
  protected readonly liste = signal<readonly StageDocument[]>([]);
  protected readonly typeChoisi = signal<DocumentType>('CONVENTION');

  protected readonly typeLabels = DOCUMENT_TYPE_LABELS;
  protected readonly statutMeta = DOCUMENT_STATUS_META;

  /** L'attestation n'est pas deposee par l'etudiant quand elle est generee. */
  protected readonly typesDeposables: readonly DocumentType[] = [
    'CONVENTION',
    'LETTRE_AFFECTATION',
    'RAPPORT',
    'ATTESTATION',
  ];

  constructor() {
    this.internships.mine(0, 1).subscribe({
      next: (page) => {
        const d = page.content[0] ?? null;
        this.dossier.set(d);
        if (d) {
          this.recharger(d.id);
        } else {
          this.chargement.set(false);
        }
      },
      error: () => this.chargement.set(false),
    });
  }

  private recharger(internshipId: number): void {
    this.documents.forInternship(internshipId).subscribe({
      next: (docs) => {
        this.liste.set(docs);
        this.chargement.set(false);
      },
      error: () => this.chargement.set(false),
    });
  }

  protected choisirType(type: DocumentType): void {
    this.typeChoisi.set(type);
  }

  protected deposer(event: Event): void {
    const input = event.target as HTMLInputElement;
    const fichier = input.files?.[0];
    const d = this.dossier();
    if (!fichier || !d) {
      return;
    }

    this.envoi.set(true);
    this.erreur.set(null);

    this.documents.upload(d.id, this.typeChoisi(), fichier).subscribe({
      next: () => {
        this.envoi.set(false);
        input.value = '';
        this.recharger(d.id);
      },
      error: (e: { error?: { message?: string } }) => {
        this.envoi.set(false);
        input.value = '';
        this.erreur.set(e.error?.message ?? 'Dépôt refusé.');
      },
    });
  }

  protected telecharger(doc: StageDocument): void {
    this.documents.download(doc.id).subscribe({
      next: (blob) => telechargerBlob(blob, doc.originalName),
      error: () => this.erreur.set('Téléchargement impossible.'),
    });
  }

  protected supprimer(doc: StageDocument): void {
    const d = this.dossier();
    if (!d) {
      return;
    }
    this.documents.remove(doc.id).subscribe({
      next: () => this.recharger(d.id),
      error: (e: { error?: { message?: string } }) =>
        this.erreur.set(e.error?.message ?? 'Suppression impossible.'),
    });
  }

  protected taille(octets: number | null): string {
    if (!octets) {
      return '—';
    }
    return octets < 1024 * 1024
      ? `${Math.round(octets / 1024)} Ko`
      : `${(octets / 1024 / 1024).toFixed(1)} Mo`;
  }
}
