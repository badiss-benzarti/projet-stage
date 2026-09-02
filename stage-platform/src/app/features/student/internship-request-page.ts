import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { RouterLink } from '@angular/router';

import { Internship } from '../../core/models/internship.models';
import { InternshipService } from '../../core/services/internship.service';
import { Company, SupervisorOption, UserService } from '../../core/services/user.service';
import { Spinner } from '../../shared/spinner';
import { StatusBadge } from '../../shared/status-badge';

/**
 * Depot et modification de la demande de stage.
 *
 * Deux chemins. Entreprise inscrite : l'etudiant la choisit dans le
 * referentiel, puis designe son encadrant parmi ceux que cette entreprise
 * a declares. Entreprise sans compte : saisie libre des coordonnees et du
 * contact de l'encadrant, le cahier des charges exigeant de couvrir le cas
 * d'un stage trouve dans une structure qui n'a aucun compte chez nous.
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
  /** Envoi du dossier en cours, distinct de l'enregistrement. */
  protected readonly envoi = signal(false);
  protected readonly erreur = signal<string | null>(null);
  protected readonly succes = signal<string | null>(null);

  protected readonly dossier = signal<Internship | null>(null);
  protected readonly entreprises = signal<readonly Company[]>([]);
  protected readonly encadrants = signal<readonly SupervisorOption[]>([]);
  protected readonly chargementEncadrants = signal(false);
  /** Vrai quand l'etudiant saisit une entreprise hors referentiel. */
  protected readonly horsReferentiel = signal(false);

  /** L'entreprise selectionnee, pour afficher sa presentation. */
  protected readonly entrepriseChoisie = computed(() =>
    this.entreprises().find((e) => e.id === this.form.controls.companyId.value) ?? null,
  );

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
    requestedSupervisorId: [null as number | null],
    companyName: [''],
    companyAddress: [''],
    companyEmail: ['', Validators.email],
    companyPhone: [''],

    // Obligatoires uniquement hors referentiel : quand l'entreprise est
    // inscrite, l'encadrant est un compte declare, pas un contact saisi.
    contactName: [''],
    contactEmail: ['', Validators.email],
    contactPhone: [''],

    startDate: ['', Validators.required],
    endDate: ['', Validators.required],
  });

  constructor() {
    this.charger();
    this.appliquerValidateursContact();
  }

  /**
   * Le contact en entreprise n'est exige que hors referentiel.
   *
   * Sur le chemin partenaire, l'encadrant est un compte declare par
   * l'entreprise : redemander son nom et son email n'apporterait qu'une
   * seconde verite, potentiellement contradictoire.
   */
  private appliquerValidateursContact(): void {
    const nom = this.form.controls.contactName;
    const email = this.form.controls.contactEmail;

    if (this.horsReferentiel()) {
      nom.setValidators([Validators.required]);
      email.setValidators([Validators.required, Validators.email]);
    } else {
      nom.setValidators([]);
      email.setValidators([Validators.email]);
    }
    nom.updateValueAndValidity({ emitEvent: false });
    email.updateValueAndValidity({ emitEvent: false });
  }

  private charger(): void {
    this.users.companies().subscribe({
      next: (page) => this.entreprises.set(page.content),
      error: () => this.entreprises.set([]),
    });

    // Plusieurs demandes peuvent coexister. Cet ecran travaille sur le
    // brouillon en cours ; s'il n'y en a pas, il en ouvre un nouveau
    // plutot que d'afficher, verrouillee, une demande deja envoyee.
    this.internships.mine(0, 50).subscribe({
      next: (page) => {
        const d = page.content.find((x) => x.status === 'DRAFT') ?? null;
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
    if (d.companyId !== null) {
      this.chargerEncadrants(d.companyId);
    }
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
      requestedSupervisorId: d.requestedSupervisorId,
      startDate: d.startDate ?? '',
      endDate: d.endDate ?? '',
    });
    this.appliquerValidateursContact();
    if (d.status !== 'DRAFT') {
      this.form.disable();
    }
  }

  /** Bascule entre partenaire reference et saisie libre. */
  protected basculerReferentiel(hors: boolean): void {
    this.horsReferentiel.set(hors);
    this.erreur.set(null);
    if (hors) {
      this.form.patchValue({ companyId: null, requestedSupervisorId: null });
      this.encadrants.set([]);
    } else {
      this.form.patchValue({ companyAddress: '', companyEmail: '', companyPhone: '' });
    }
    this.appliquerValidateursContact();
  }

  /**
   * Recopie les coordonnees du partenaire choisi, puis recharge la liste
   * de ses encadrants : celle du partenaire precedent n'a plus de sens.
   */
  protected choisirPartenaire(id: string): void {
    const identifiant = Number(id);
    const partenaire = this.entreprises().find((c) => c.id === identifiant);
    this.form.patchValue({
      companyId: identifiant,
      companyName: partenaire?.name ?? '',
      companyAddress: partenaire?.address ?? '',
      companyEmail: partenaire?.email ?? '',
      companyPhone: partenaire?.phone ?? '',
      requestedSupervisorId: null,
    });
    this.chargerEncadrants(identifiant);
  }

  protected choisirEncadrant(id: string): void {
    this.form.patchValue({ requestedSupervisorId: id ? Number(id) : null });
    this.erreur.set(null);
  }

  private chargerEncadrants(companyId: number): void {
    this.chargementEncadrants.set(true);
    this.users.supervisorOptionsOf(companyId).subscribe({
      next: (liste) => {
        this.encadrants.set(liste);
        this.chargementEncadrants.set(false);
      },
      error: () => {
        this.encadrants.set([]);
        this.chargementEncadrants.set(false);
      },
    });
  }

  /**
   * L'action que le backend propose pour envoyer le dossier.
   *
   * On ne code pas "SUBMITTED" en dur : le frontend se contente des
   * availableActions, et suit donc le workflow s'il change.
   */
  protected readonly actionEnvoi = computed(
    () => this.dossier()?.availableActions?.[0] ?? null,
  );

  /** Envoie le dossier, apres l'avoir enregistre s'il a ete modifie. */
  protected envoyer(): void {
    const d = this.dossier();
    const action = this.actionEnvoi();
    if (!d || !action || this.envoi()) {
      return;
    }
    if (this.form.dirty) {
      this.erreur.set('Enregistrez d’abord vos modifications, puis envoyez.');
      return;
    }

    this.envoi.set(true);
    this.erreur.set(null);
    this.succes.set(null);

    this.internships.transition(d.id, { target: action.target }).subscribe({
      next: (maj) => {
        this.dossier.set(maj);
        this.envoi.set(false);
        this.succes.set('Demande envoyée.');
      },
      error: (e: { error?: { message?: string } }) => {
        this.envoi.set(false);
        this.erreur.set(e.error?.message ?? 'Envoi impossible.');
      },
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
    if (!this.horsReferentiel() && v.requestedSupervisorId === null) {
      this.erreur.set(
        this.encadrants().length === 0
          ? 'Cette entreprise n’a déclaré aucun encadrant. Contactez le service des stages.'
          : 'Choisissez l’encadrant qui vous suivra dans l’entreprise.',
      );
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
      requestedSupervisorId: this.horsReferentiel() ? null : v.requestedSupervisorId,
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
            : 'Brouillon créé. Vous pouvez l’envoyer ci-dessous.',
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
