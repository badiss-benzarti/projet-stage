import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core';

import {
  AvailableAction,
  Internship,
  InternshipStatus,
  STATUS_META,
} from '../../core/models/internship.models';
import { DocumentService } from '../../core/services/document.service';
import { InternshipService } from '../../core/services/internship.service';
import { InternshipTable } from '../../shared/internship-table';
import { Spinner } from '../../shared/spinner';
import { StatusBadge } from '../../shared/status-badge';

/**
 * Instruction des demandes par le service des stages.
 *
 * La liste est filtrable par etat ; le detail affiche les actions que le
 * backend autorise pour ce role et cet etat, avec le motif exige quand
 * la transition le demande.
 */
@Component({
  selector: 'gs-internship-review-page',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [InternshipTable, StatusBadge, Spinner],
  templateUrl: './internship-review-page.html',
})
export class InternshipReviewPage {
  private readonly internships = inject(InternshipService);
  private readonly documents = inject(DocumentService);

  protected readonly chargement = signal(true);
  protected readonly envoi = signal(false);
  protected readonly erreur = signal<string | null>(null);

  protected readonly dossiers = signal<readonly Internship[]>([]);
  protected readonly filtre = signal<InternshipStatus | null>(null);
  protected readonly selection = signal<Internship | null>(null);
  protected readonly motif = signal('');
  protected readonly actionEnAttente = signal<AvailableAction | null>(null);

  protected readonly meta = STATUS_META;

  protected readonly attestationEnCours = signal(false);
  protected readonly attestationMessage = signal<string | null>(null);

  /**
   * L'attestation n'est generable que pour les structures internes et sur
   * un stage cloture. Le backend applique la meme regle : on n'affiche le
   * bouton que quand il a une chance d'aboutir.
   */
  protected readonly attestationPossible = computed(() => {
    const d = this.selection();
    if (!d || d.status !== 'COMPLETED') {
      return false;
    }
    const nom = (d.companyName ?? '').toLowerCase();
    return nom.includes('dsi') || nom.includes('espritech') || nom.includes('esprittech');
  });

  protected genererAttestation(): void {
    const d = this.selection();
    if (!d || this.attestationEnCours()) {
      return;
    }
    this.attestationEnCours.set(true);
    this.attestationMessage.set(null);
    this.erreur.set(null);

    this.documents.generateAttestation(d.id).subscribe({
      next: (doc) => {
        this.attestationEnCours.set(false);
        this.attestationMessage.set(
          `Attestation générée (${doc.originalName}) et mise à disposition de l’étudiant.`,
        );
      },
      error: (e: { error?: { message?: string } }) => {
        this.attestationEnCours.set(false);
        this.erreur.set(e.error?.message ?? 'Génération impossible.');
      },
    });
  }

  protected readonly filtres: readonly (InternshipStatus | null)[] = [
    null,
    'SUBMITTED',
    'UNDER_REVIEW',
    'APPROVED',
    'COMPANY_PENDING',
    'IN_PROGRESS',
    'COMPLETED',
  ];

  protected readonly compteurs = computed(() => {
    const totaux = new Map<InternshipStatus, number>();
    for (const d of this.dossiers()) {
      totaux.set(d.status, (totaux.get(d.status) ?? 0) + 1);
    }
    return totaux;
  });

  constructor() {
    this.charger();
  }

  protected charger(): void {
    this.chargement.set(true);
    this.internships.forDepartment(this.filtre() ?? undefined, 0, 100).subscribe({
      next: (page) => {
        this.dossiers.set(page.content);
        this.chargement.set(false);
      },
      error: () => this.chargement.set(false),
    });
  }

  protected filtrer(statut: InternshipStatus | null): void {
    this.filtre.set(statut);
    this.selection.set(null);
    this.charger();
  }

  /** Le detail complet, avec actions et historique, vient d'un GET dedie. */
  protected ouvrir(dossier: Internship): void {
    this.erreur.set(null);
    this.annulerAction();
    this.internships.byId(dossier.id).subscribe({
      next: (complet) => this.selection.set(complet),
      error: () => this.erreur.set('Impossible de charger ce dossier.'),
    });
  }

  protected fermer(): void {
    this.selection.set(null);
    this.annulerAction();
  }

  protected preparer(action: AvailableAction): void {
    if (action.requiresReason) {
      this.actionEnAttente.set(action);
      this.motif.set('');
      return;
    }
    this.executer(action);
  }

  protected annulerAction(): void {
    this.actionEnAttente.set(null);
    this.motif.set('');
  }

  protected confirmer(): void {
    const action = this.actionEnAttente();
    if (!action) {
      return;
    }
    if (this.motif().trim().length === 0) {
      this.erreur.set('Un motif est obligatoire pour cette décision.');
      return;
    }
    this.executer(action, this.motif().trim());
  }

  private executer(action: AvailableAction, commentaire?: string): void {
    const dossier = this.selection();
    if (!dossier || this.envoi()) {
      return;
    }
    this.envoi.set(true);
    this.erreur.set(null);

    this.internships
      .transition(dossier.id, { target: action.target, comment: commentaire })
      .subscribe({
        next: (maj) => {
          this.selection.set(maj);
          this.dossiers.update((liste) => liste.map((d) => (d.id === maj.id ? maj : d)));
          this.envoi.set(false);
          this.annulerAction();
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
