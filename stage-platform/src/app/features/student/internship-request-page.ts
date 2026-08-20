import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { RouterLink } from '@angular/router';

import { Internship } from '../../core/models/internship.models';
import { InternshipService } from '../../core/services/internship.service';
import { Company, UserService } from '../../core/services/user.service';
import { Spinner } from '../../shared/spinner';
import { StatusBadge } from '../../shared/status-badge';

/**
 * Depot et modification de la demande de stage.
 *
 * Le formulaire n'est modifiable qu'a l'etat DRAFT : cette regle vient
 * du backend, on se contente de la refleter en desactivant les champs.
 */
@Component({
  selector: 'gs-internship-request-page',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [ReactiveFormsModule, RouterLink, StatusBadge, Spinner],
  templateUrl: './internship-request-page.html',
})
export class InternshipRequestPage {
  private readonly fb = inject(FormBuilder);
  private readonly internships = inject(InternshipService);
  private readonly users = inject(UserService);

  protected readonly chargement = signal(true);
  protected readonly enregistrement = signal(false);
  protected readonly erreur = signal<string | null>(null);
  protected readonly succes = signal<string | null>(null);

  protected readonly dossier = signal<Internship | null>(null);
  protected readonly entreprises = signal<readonly Company[]>([]);

  protected readonly modifiable = computed(() => {
    const d = this.dossier();
    return d === null || d.status === 'DRAFT';
  });

  protected readonly anneesUniversitaires = this.calculerAnnees();

  protected readonly form = this.fb.nonNullable.group({
    type: ['PFE' as 'PFE' | 'ETE', Validators.required],
    title: ['', [Validators.required, Validators.maxLength(200)]],
    description: [''],
    academicYear: [this.anneesUniversitaires[0], Validators.required],
    companyId: [null as number | null, Validators.required],
    startDate: ['', Validators.required],
    endDate: ['', Validators.required],
  });

  constructor() {
    this.charger();
  }

  private charger(): void {
    this.users.companies().subscribe({
      next: (page) => this.entreprises.set(page.content),
      error: () => this.entreprises.set([]),
    });

    this.internships.mine(0, 1).subscribe({
      next: (page) => {
        const d = page.content.length > 0 ? page.content[0] : null;
        this.dossier.set(d);
        if (d) {
          this.remplir(d);
        }
        this.chargement.set(false);
      },
      error: (e: { error?: { message?: string } }) => {
        this.chargement.set(false);
        this.erreur.set(e.error?.message ?? 'Impossible de charger votre dossier.');
      },
    });
  }

  private remplir(d: Internship): void {
    this.form.patchValue({
      type: d.type,
      title: d.title,
      description: d.description ?? '',
      academicYear: d.academicYear,
      companyId: d.companyId,
      startDate: d.startDate ?? '',
      endDate: d.endDate ?? '',
    });
    if (d.status !== 'DRAFT') {
      this.form.disable();
    }
  }

  protected soumettre(): void {
    if (this.form.invalid || this.enregistrement()) {
      this.form.markAllAsTouched();
      return;
    }

    const valeurs = this.form.getRawValue();
    const entreprise = this.entreprises().find((c) => c.id === valeurs.companyId);

    const demande = {
      type: valeurs.type,
      title: valeurs.title.trim(),
      description: valeurs.description.trim() || null,
      academicYear: valeurs.academicYear,
      companyId: valeurs.companyId,
      companyName: entreprise?.name ?? null,
      startDate: valeurs.startDate || null,
      endDate: valeurs.endDate || null,
    };

    this.enregistrement.set(true);
    this.erreur.set(null);
    this.succes.set(null);

    const existant = this.dossier();
    const appel = existant
      ? this.internships.updateDraft(existant.id, demande)
      : this.internships.create(demande);

    appel.subscribe({
      next: (d) => {
        this.dossier.set(d);
        this.enregistrement.set(false);
        this.succes.set(
          existant ? 'Demande enregistrée.' : 'Brouillon créé. Soumettez-le depuis le tableau de bord.',
        );
      },
      error: (e: { error?: { message?: string; champs?: Record<string, string> } }) => {
        this.enregistrement.set(false);
        const champs = e.error?.champs;
        this.erreur.set(
          champs
            ? Object.values(champs).join(' · ')
            : (e.error?.message ?? 'Enregistrement impossible.'),
        );
      },
    });
  }

  /** Trois annees universitaires autour de l'annee courante. */
  private calculerAnnees(): readonly string[] {
    const a = new Date().getFullYear();
    return [`${a}-${a + 1}`, `${a - 1}-${a}`, `${a + 1}-${a + 2}`];
  }
}
