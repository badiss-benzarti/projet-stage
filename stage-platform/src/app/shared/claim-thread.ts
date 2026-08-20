import { ChangeDetectionStrategy, Component, computed, input, output, signal } from '@angular/core';
import { DatePipe } from '@angular/common';

import { Claim, CLAIM_STATUS_LABELS, ClaimStatus } from '../core/models/evaluation.models';

/**
 * Fil d'une reclamation, partage par l'etudiant et le departement.
 *
 * Le bouclage exige par le cahier des charges se voit ici : les messages
 * alternent, le compteur de relances est affiche, et une reclamation
 * close n'accepte plus de reponse.
 */
@Component({
  selector: 'gs-claim-thread',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [DatePipe],
  templateUrl: './claim-thread.html',
})
export class ClaimThread {
  readonly claim = input.required<Claim>();
  /** Role de l'utilisateur courant, pour aligner ses messages a droite. */
  readonly myRole = input.required<string>();
  readonly canClose = input<boolean>(false);
  readonly busy = input<boolean>(false);

  readonly reply = output<string>();
  readonly close = output<string>();

  protected readonly brouillon = signal('');
  protected readonly statusLabels = CLAIM_STATUS_LABELS;

  protected readonly close_ = computed(() => this.claim().status === 'CLOSED');

  protected readonly pastille = computed(() => this.tonPour(this.claim().status));

  protected majBrouillon(valeur: string): void {
    this.brouillon.set(valeur);
  }

  protected envoyer(): void {
    const texte = this.brouillon().trim();
    if (texte.length === 0) {
      return;
    }
    this.reply.emit(texte);
    this.brouillon.set('');
  }

  protected cloturer(): void {
    this.close.emit(this.brouillon().trim());
    this.brouillon.set('');
  }

  private tonPour(statut: ClaimStatus): string {
    switch (statut) {
      case 'CLOSED':
        return 'border-sand-300 bg-sand-100 text-sand-600';
      case 'RESPONDED':
        return 'border-ok-500/25 bg-ok-50 text-ok-700';
      case 'REOPENED':
        return 'border-bad-500/25 bg-bad-50 text-bad-700';
      default:
        return 'border-warn-500/25 bg-warn-50 text-warn-700';
    }
  }
}
