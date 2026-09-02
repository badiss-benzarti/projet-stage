import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';

import { AvailableAction, Internship } from '../../core/models/internship.models';
import {
  DOCUMENT_TYPE_LABELS,
  DocumentService,
  StageDocument,
} from '../../core/services/document.service';
import { InternshipService } from '../../core/services/internship.service';
import { Supervisor, UserService } from '../../core/services/user.service';
import { telechargerBlob } from '../../shared/download';
import { EmptyState } from '../../shared/empty-state';
import { Spinner } from '../../shared/spinner';
import { StatusBadge } from '../../shared/status-badge';
import { StudentProfileCard } from '../../shared/student-profile-card';

/**
 * Demandes transmises a l'entreprise.
 *
 * Accepter exige de designer un encadrant : c'est lui qui validera le
 * journal et remplira la grille. Le backend refuse une acceptation sans
 * encadrant, on impose donc le choix ici.
 *
 * L'etudiant a generalement deja propose un encadrant a sa demande : on
 * le pre-selectionne, sans le figer. L'entreprise reste maitresse de
 * l'affectation de ses collaborateurs, elle peut en designer un autre.
 */
@Component({
  selector: 'gs-company-requests-page',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [StatusBadge, StudentProfileCard, EmptyState, Spinner],
  templateUrl: './company-requests-page.html',
})
export class CompanyRequestsPage {
  private readonly internships = inject(InternshipService);
  private readonly users = inject(UserService);
  private readonly documents = inject(DocumentService);

  protected readonly typeLabels = DOCUMENT_TYPE_LABELS;

  protected readonly chargement = signal(true);
  protected readonly envoi = signal(false);
  protected readonly erreur = signal<string | null>(null);

  protected readonly demandes = signal<readonly Internship[]>([]);
  protected readonly encadrants = signal<readonly Supervisor[]>([]);

  protected readonly acceptationEnCours = signal<number | null>(null);
  protected readonly refusEnCours = signal<number | null>(null);
  protected readonly encadrantChoisi = signal<number | null>(null);
  protected readonly motif = signal('');

  constructor() {
    this.internships.forCompany('COMPANY_PENDING', 0, 50).subscribe({
      next: (page) => {
        this.demandes.set(page.content);
        this.chargement.set(false);
        page.content.forEach((d) => this.chargerDossier(d));
      },
      error: (e: { error?: { message?: string } }) => {
        this.chargement.set(false);
        this.erreur.set(e.error?.message ?? 'Impossible de charger les demandes.');
      },
    });

    this.users.myCompany().subscribe({
      next: (c) =>
        this.users.supervisorsOf(c.id).subscribe({
          next: (liste) => this.encadrants.set(liste),
        }),
      error: () => this.encadrants.set([]),
    });
  }

  /**
   * Pieces jointes a la demande, par dossier.
   *
   * Chargees a l'ouverture de la liste plutot qu'au clic : l'entreprise
   * doit voir d'un coup d'oeil si un candidat a joint quelque chose,
   * sans avoir a deplier chaque demande pour le decouvrir.
   */
  protected readonly piecesParDossier = signal<ReadonlyMap<number, readonly StageDocument[]>>(
    new Map(),
  );

  private chargerDossier(d: Internship): void {
    this.documents.forInternship(d.id).subscribe({
      next: (liste) => {
        const copie = new Map(this.piecesParDossier());
        copie.set(d.id, liste);
        this.piecesParDossier.set(copie);
      },
      error: () => {
        /* Une piece illisible ne doit pas masquer la demande elle-meme. */
      },
    });
  }

  protected pieces(dossier: Internship): readonly StageDocument[] {
    return this.piecesParDossier().get(dossier.id) ?? [];
  }

  protected telecharger(doc: StageDocument): void {
    this.documents.download(doc.id).subscribe({
      next: (blob) => telechargerBlob(blob, doc.originalName ?? 'document.pdf'),
      error: () => this.erreur.set('Téléchargement impossible.'),
    });
  }

  protected commencerAcceptation(dossier: Internship): void {
    this.refusEnCours.set(null);
    this.acceptationEnCours.set(dossier.id);
    this.encadrantChoisi.set(
      dossier.requestedSupervisorId ?? this.encadrants()[0]?.id ?? null,
    );
    this.erreur.set(null);
  }

  protected commencerRefus(dossier: Internship): void {
    this.acceptationEnCours.set(null);
    this.refusEnCours.set(dossier.id);
    this.motif.set('');
    this.erreur.set(null);
  }

  protected annuler(): void {
    this.acceptationEnCours.set(null);
    this.refusEnCours.set(null);
    this.motif.set('');
  }

  protected choisirEncadrant(id: string): void {
    this.encadrantChoisi.set(Number(id));
  }

  protected majMotif(valeur: string): void {
    this.motif.set(valeur);
  }

  protected confirmerAcceptation(dossier: Internship): void {
    const encadrantId = this.encadrantChoisi();
    if (!encadrantId) {
      this.erreur.set('Désignez un encadrant : il validera le journal et remplira la grille.');
      return;
    }
    const encadrant = this.encadrants().find((e) => e.id === encadrantId);
    this.appliquer(dossier, {
      target: 'ACCEPTED',
      supervisorId: encadrantId,
      supervisorName: encadrant ? `${encadrant.firstName} ${encadrant.lastName}` : undefined,
    });
  }

  protected confirmerRefus(dossier: Internship): void {
    if (this.motif().trim().length === 0) {
      this.erreur.set('Un motif est obligatoire pour refuser un stagiaire.');
      return;
    }
    this.appliquer(dossier, { target: 'REFUSED', comment: this.motif().trim() });
  }

  private appliquer(
    dossier: Internship,
    requete: { target: 'ACCEPTED' | 'REFUSED'; comment?: string; supervisorId?: number; supervisorName?: string },
  ): void {
    if (this.envoi()) {
      return;
    }
    this.envoi.set(true);
    this.erreur.set(null);

    this.internships.transition(dossier.id, requete).subscribe({
      next: () => {
        // Le dossier quitte la file d'attente une fois tranche.
        this.demandes.update((liste) => liste.filter((d) => d.id !== dossier.id));
        this.envoi.set(false);
        this.annuler();
      },
      error: (e: { error?: { message?: string } }) => {
        this.envoi.set(false);
        this.erreur.set(e.error?.message ?? 'Action impossible.');
      },
    });
  }

  protected actionsDe(dossier: Internship): readonly AvailableAction[] {
    return dossier.availableActions;
  }
}
