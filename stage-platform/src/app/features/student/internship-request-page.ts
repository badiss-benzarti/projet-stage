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
 * L'entreprise d'accueil peut etre choisie dans le referentiel des
 * partenaires, ou saisie librement : un etudiant trouve souvent son
 * stage dans une structure qui n'a aucun compte sur la plateforme.
 * Le cahier des charges exige ce remplissage libre, ainsi que le
 * contact de l'encadrant.
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
  /** Vrai quand l'etudiant saisit une entreprise hors referentiel. */
  protected readonly horsReferentiel = signal(false);

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

    companyId: [null as number | null],
    companyName: [''],
    companyAddress: [''],
    companyEmail: ['', Validators.email],
    companyPhone: [''],

    contactName: ['', Validators.required],
    contactEmail: ['', [Validators.required, Validators.email]],
    contactPhone: [''],

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
    this.horsReferentiel.set(d.companyId === null);
    this.form.patchValue({
      type: d.type,
      title: d.title,
      description: d.description ?? '',
      academicYear: d.academicYear,
      companyId: d.companyId,
      companyName: d.companyName ?? '',
      companyAddress: d.companyAddress ?? '',
      companyEmail: d.companyEmail ?? '',
      companyPhone: d.companyPhone ?? '',
      contactName: d.contactName ?? '',
      contactEmail: d.contactEmail ?? '',
      contactPhone: d.contactPhone ?? '',
      startDate: d.startDate ?? '',
      endDate: d.endDate ?? '',
    });
    if (d.status !== 'DRAFT') {
      this.form.disable();
    }
  }

  /** Bascule entre partenaire reference et saisie libre. */
  protected basculerReferentiel(hors: boolean): void {
    this.horsReferentiel.set(hors);
    this.erreur.set(null);
    if (hors) {
      this.form.patchValue({ companyId: null });
    } else {
      this.form.patchValue({ companyAddress: '', companyEmail: '', companyPhone: '' });
    }
  }

  /** Recopie les coordonnees du partenaire choisi, pour information. */
  protected choisirPartenaire(id: string): void {
    const identifiant = Number(id);
    const partenaire = this.entreprises().find((c) => c.id === identifiant);
    this.form.patchValue({
      companyId: identifiant,
      companyName: partenaire?.name ?? '',
      companyAddress: partenaire?.address ?? '',
      companyEmail: partenaire?.email ?? '',
      companyPhone: partenaire?.phone ?? '',
    });
  }

  protected soumettre(): void {
    if (this.enregistrement()) {
      return;
    }
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      this.erreur.set('Complétez les champs obligatoires.');
      return;
    }
    const v = this.form.getRawValue();

    if (!this.horsReferentiel() && v.companyId === null) {
      this.erreur.set('Choisissez une entreprise partenaire, ou saisissez-en une.');
      return;
    }
    if (this.horsReferentiel() && v.companyName.trim().length === 0) {
      this.erreur.set('Renseignez le nom de l’entreprise d’accueil.');
      return;
    }

    const demande = {
      type: v.type,
      title: v.title.trim(),
      description: v.description.trim() || null,
      academicYear: v.academicYear,
      companyId: this.horsReferentiel() ? null : v.companyId,
      companyName: v.companyName.trim() || null,
      companyAddress: v.companyAddress.trim() || null,
      companyEmail: v.companyEmail.trim() || null,
      companyPhone: v.companyPhone.trim() || null,
      contactName: v.contactName.trim() || null,
      contactEmail: v.contactEmail.trim() || null,
      contactPhone: v.contactPhone.trim() || null,
      startDate: v.startDate || null,
      endDate: v.endDate || null,
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
          existant
            ? 'Demande enregistrée.'
            : 'Brouillon créé. Soumettez-le depuis le tableau de bord.',
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

  private calculerAnnees(): readonly string[] {
    const a = new Date().getFullYear();
    return [`${a}-${a + 1}`, `${a - 1}-${a}`, `${a + 1}-${a + 2}`];
  }
}
