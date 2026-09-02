import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';

import { DocumentRequest, Internship } from '../../core/models/internship.models';
import { DocumentService } from '../../core/services/document.service';
import { InternshipService } from '../../core/services/internship.service';
import { InternshipTable } from '../../shared/internship-table';
import { Spinner } from '../../shared/spinner';
import { StatusBadge } from '../../shared/status-badge';
import { StudentProfileCard } from '../../shared/student-profile-card';

/** Tous les stages accueillis par l'entreprise. */
@Component({
  selector: 'gs-company-interns-page',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [InternshipTable, StatusBadge, StudentProfileCard, Spinner],
  templateUrl: './company-interns-page.html',
})
export class CompanyInternsPage {
  private readonly internships = inject(InternshipService);
  private readonly documents = inject(DocumentService);

  protected readonly chargement = signal(true);
  protected readonly dossiers = signal<readonly Internship[]>([]);
  protected readonly selection = signal<Internship | null>(null);

  /** Attestations de stage que les stagiaires reclament a l'entreprise. */
  protected readonly attestations = signal<readonly DocumentRequest[]>([]);
  protected readonly envoi = signal<number | null>(null);
  protected readonly erreur = signal<string | null>(null);
  protected readonly succes = signal<string | null>(null);

  constructor() {
    this.internships.forCompany(undefined, 0, 100).subscribe({
      next: (page) => {
        this.dossiers.set(page.content);
        this.chargement.set(false);
      },
      error: () => this.chargement.set(false),
    });

    this.chargerAttestations();
  }

  private chargerAttestations(): void {
    this.internships.companyRequests().subscribe({
      next: (liste) => this.attestations.set(liste),
      error: () => this.attestations.set([]),
    });
  }

  /**
   * Delivrer l'attestation : accepter la demande, produire le PDF, puis
   * le rattacher. Les trois etapes vivent dans deux services distincts,
   * d'ou l'enchainement ici. Si l'edition echoue, la demande reste
   * acceptee sans document et le message le dit plutot que de laisser
   * croire a une reussite.
   */
  protected delivrer(demande: DocumentRequest): void {
    // Une attestation de stage porte toujours sur un dossier ; la garde
    // n'existe que pour satisfaire le type, partage avec les demandes
    // sans dossier comme l'attestation de scolarite.
    const internshipId = demande.internshipId;
    if (this.envoi() !== null || internshipId === null) {
      return;
    }
    this.envoi.set(demande.id);
    this.erreur.set(null);
    this.succes.set(null);

    this.internships.decideRequest(demande.id, 'ISSUED').subscribe({
      next: () =>
        this.documents.generateAttestation(internshipId).subscribe({
          next: (doc) =>
            this.internships.markRequestIssued(demande.id, doc.id).subscribe({
              next: () => this.terminer(demande.id),
              error: () => this.terminer(demande.id),
            }),
          error: (e: { error?: { message?: string } }) => {
            this.envoi.set(null);
            this.erreur.set(
              'Demande acceptée, mais l’attestation n’a pas pu être éditée : ' +
                (e.error?.message ?? 'erreur inconnue'),
            );
          },
        }),
      error: (e: { error?: { message?: string } }) => {
        this.envoi.set(null);
        this.erreur.set(e.error?.message ?? 'Impossible de délivrer l’attestation.');
      },
    });
  }

  private terminer(id: number): void {
    this.attestations.update((liste) => liste.filter((d) => d.id !== id));
    this.envoi.set(null);
    this.succes.set('Attestation délivrée. Le stagiaire peut la télécharger.');
  }

  protected refuser(demande: DocumentRequest): void {
    if (this.envoi() !== null) {
      return;
    }
    this.envoi.set(demande.id);
    this.erreur.set(null);

    this.internships
      .decideRequest(demande.id, 'REJECTED', 'Attestation non delivree par l’entreprise')
      .subscribe({
        next: () => {
          this.attestations.update((liste) => liste.filter((d) => d.id !== demande.id));
          this.envoi.set(null);
        },
        error: (e: { error?: { message?: string } }) => {
          this.envoi.set(null);
          this.erreur.set(e.error?.message ?? 'Refus impossible.');
        },
      });
  }

  protected ouvrir(dossier: Internship): void {
    this.internships.byId(dossier.id).subscribe({
      next: (complet) => this.selection.set(complet),
    });
  }

  protected fermer(): void {
    this.selection.set(null);
  }
}
