import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';

import { AvailableAction, Internship } from '../../core/models/internship.models';
import { InternshipService } from '../../core/services/internship.service';
import { Supervisor, UserService } from '../../core/services/user.service';
import { EmptyState } from '../../shared/empty-state';
import { Spinner } from '../../shared/spinner';
import { StatusBadge } from '../../shared/status-badge';

/**
 * Demandes transmises a l'entreprise.
 *
 * Accepter exige de designer un encadrant : c'est lui qui validera le
 * journal et remplira la grille. Le backend refuse une acceptation sans
 * encadrant, on impose donc le choix ici.
 */
@Component({
  selector: 'gs-company-requests-page',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [StatusBadge, EmptyState, Spinner],
  templateUrl: './company-requests-page.html',
})
export class CompanyRequestsPage {
  private readonly internships = inject(InternshipService);
  private readonly users = inject(UserService);

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

  protected commencerAcceptation(dossier: Internship): void {
    this.refusEnCours.set(null);
    this.acceptationEnCours.set(dossier.id);
    this.encadrantChoisi.set(this.encadrants()[0]?.id ?? null);
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
