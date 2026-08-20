import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';

import { Claim } from '../../core/models/evaluation.models';
import { EvaluationService } from '../../core/services/evaluation.service';
import { AuthService } from '../../core/services/auth.service';
import { ClaimThread } from '../../shared/claim-thread';
import { EmptyState } from '../../shared/empty-state';
import { Spinner } from '../../shared/spinner';

/** Traitement des reclamations par le departement pedagogique. */
@Component({
  selector: 'gs-department-claims-page',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [ClaimThread, EmptyState, Spinner],
  templateUrl: './department-claims-page.html',
})
export class DepartmentClaimsPage {
  private readonly evaluations = inject(EvaluationService);
  private readonly auth = inject(AuthService);

  protected readonly chargement = signal(true);
  protected readonly envoi = signal(false);
  protected readonly erreur = signal<string | null>(null);
  protected readonly reclamations = signal<readonly Claim[]>([]);

  protected readonly monRole = this.auth.role() ?? 'CHEF_DEPARTEMENT_PEDAGOGIQUE';

  constructor() {
    this.evaluations.departmentClaims().subscribe({
      next: (page) => {
        this.reclamations.set([]);
        page.content.forEach((c) =>
          this.evaluations.claim(c.id).subscribe({
            next: (complet) => {
              this.reclamations.update((liste) => [...liste, complet]);
              // Une reclamation ouverte est prise en charge des sa
              // consultation : l'etudiant voit que son dossier avance.
              if (complet.status === 'OPEN' || complet.status === 'REOPENED') {
                this.prendreEnCharge(complet);
              }
            },
          }),
        );
        this.chargement.set(false);
      },
      error: () => this.chargement.set(false),
    });
  }

  private prendreEnCharge(claim: Claim): void {
    this.evaluations.takeClaim(claim.id).subscribe({
      next: (maj) => this.remplacer(maj),
      error: () => undefined,
    });
  }

  protected repondre(claim: Claim, contenu: string): void {
    this.envoi.set(true);
    this.evaluations.replyToClaim(claim.id, contenu).subscribe({
      next: (maj) => {
        this.remplacer(maj);
        this.envoi.set(false);
      },
      error: (e: { error?: { message?: string } }) => {
        this.envoi.set(false);
        this.erreur.set(e.error?.message ?? 'Envoi impossible.');
      },
    });
  }

  protected cloturer(claim: Claim, contenu: string): void {
    this.envoi.set(true);
    this.evaluations.closeClaim(claim.id, contenu).subscribe({
      next: (maj) => {
        this.remplacer(maj);
        this.envoi.set(false);
      },
      error: (e: { error?: { message?: string } }) => {
        this.envoi.set(false);
        this.erreur.set(e.error?.message ?? 'Clôture impossible.');
      },
    });
  }

  private remplacer(maj: Claim): void {
    this.reclamations.update((liste) => liste.map((c) => (c.id === maj.id ? maj : c)));
  }
}
