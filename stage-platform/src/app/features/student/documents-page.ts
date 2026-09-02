import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core';

import {
  DocumentRequest,
  Internship,
  estClose,
  REQUEST_STATUS_LABELS,
  REQUEST_TYPE_LABELS,
  RequestType,
} from '../../core/models/internship.models';
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

  /**
   * Deux vues de la meme liste : ce que l'etudiant a remis, et ce que
   * l'ecole ou l'entreprise a edite pour lui. Le drapeau generated est
   * pose par le backend au moment de produire le PDF, c'est donc lui qui
   * fait foi et non le type du document.
   */
  protected readonly deposes = computed(() => this.liste().filter((d) => !d.generated));
  protected readonly recus = computed(() => this.liste().filter((d) => d.generated));
  protected readonly typeChoisi = signal<DocumentType>('JOURNAL');

  // --- Demandes administratives : convention et lettre d'affectation ---
  protected readonly demandes = signal<readonly DocumentRequest[]>([]);
  protected readonly demandeEnCours = signal(false);
  protected readonly requestTypeLabels = REQUEST_TYPE_LABELS;
  protected readonly requestStatusLabels = REQUEST_STATUS_LABELS;
  /**
   * Ce que l'etudiant reclame. Les deux premiers sont edites par l'ecole,
   * l'attestation de stage par l'entreprise d'accueil : la demande ne
   * part donc pas au meme endroit, mais elle se formule ici de la meme
   * facon - l'etudiant n'a pas a savoir qui la traite.
   */
  protected readonly typesDemandables: readonly RequestType[] = [
    'CONVENTION',
    'LETTRE_AFFECTATION',
    'ATTESTATION_STAGE',
  ];

  protected readonly typeLabels = DOCUMENT_TYPE_LABELS;
  protected readonly statutMeta = DOCUMENT_STATUS_META;

  /**
   * Ce que l'etudiant rend au service des stages, en fin de stage.
   *
   * Trois pieces seulement : le journal une fois signe par le maitre de
   * stage et cachete par l'entreprise, le rapport, et l'attestation que
   * l'entreprise lui remet. Le reste - convention, lettre d'affectation
   * - est EDITE par l'ecole et se reclame plus bas, dans les demandes de
   * documents : le proposer ici laissait croire que l'etudiant devait
   * fournir ce qu'il vient justement demander.
   */
  protected readonly typesDeposables: readonly DocumentType[] = [
    'JOURNAL',
    'RAPPORT',
    'ATTESTATION',
  ];

  /**
   * Tous les dossiers de l'etudiant, pas seulement le dernier.
   *
   * Depuis qu'il peut deposer plusieurs demandes, ses documents sont
   * repartis entre elles : une attestation editee sur un stage clos
   * n'apparaissait plus des lors qu'un autre dossier etait ouvert. On
   * rassemble donc les documents de tous ses dossiers.
   */
  private readonly dossiers = signal<readonly Internship[]>([]);

  constructor() {
    this.internships.mine(0, 50).subscribe({
      next: (page) => {
        this.dossiers.set(page.content);

        // Le depot et les demandes portent sur le dossier en cours :
        // celui qui l'engage, sinon le plus recent encore ouvert.
        const courant =
          page.content.find((d) => d.status === 'IN_PROGRESS' || d.status === 'ACCEPTED') ??
          page.content.find((d) => !estClose(d.status)) ??
          page.content[0] ??
          null;
        this.dossier.set(courant);

        this.rechargerDocuments();
        this.rechargerDemandes();

        if (page.content.length === 0) {
          this.chargement.set(false);
        }
      },
      error: () => this.chargement.set(false),
    });
  }

  private rechargerDocuments(): void {
    const dossiers = this.dossiers();
    if (dossiers.length === 0) {
      this.liste.set([]);
      this.chargement.set(false);
      return;
    }

    // Un appel par dossier : le document-service n'expose pas de
    // recherche par etudiant, ses documents sont indexes par stage.
    let restants = dossiers.length;
    const cumul: StageDocument[] = [];

    for (const d of dossiers) {
      this.documents.forInternship(d.id).subscribe({
        next: (docs) => cumul.push(...docs),
        error: () => {
          /* Un dossier illisible ne doit pas vider toute la liste. */
        },
        complete: () => {
          restants -= 1;
          if (restants === 0) {
            this.liste.set(cumul);
            this.chargement.set(false);
          }
        },
      });
    }
  }

  /** Toutes ses demandes, y compris celles sans dossier rattache. */
  private rechargerDemandes(): void {
    this.internships.myRequests().subscribe({
      next: (liste) => this.demandes.set(liste),
      error: () => this.demandes.set([]),
    });
  }

  /**
   * Demande de convention ou de lettre d'affectation.
   *
   * Distinct du depot : ici l'etudiant DEMANDE un document que le service
   * des stages editera ; le depot concerne le fichier signe qu'il rapporte.
   */
  protected demander(type: RequestType): void {
    const d = this.dossier();
    if (!d || this.demandeEnCours()) {
      return;
    }
    this.demandeEnCours.set(true);
    this.erreur.set(null);

    this.internships.askDocument(d.id, type).subscribe({
      next: (demande) => {
        this.demandes.update((liste) => [demande, ...liste]);
        this.demandeEnCours.set(false);
      },
      error: (e: { error?: { message?: string } }) => {
        this.demandeEnCours.set(false);
        this.erreur.set(e.error?.message ?? 'Demande impossible.');
      },
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
        this.rechargerDocuments();
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
      next: () => this.rechargerDocuments(),
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
