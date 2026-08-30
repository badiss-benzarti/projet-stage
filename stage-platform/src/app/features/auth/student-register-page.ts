import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';

import { AuthService } from '../../core/services/auth.service';
import { Option, UserService } from '../../core/services/user.service';
import { SessionNotice } from '../../shared/session-notice';

/**
 * Inscription d'un etudiant, en deux temps.
 *
 * Le dossier de scolarite est demande des l'inscription plutot qu'apres :
 * sans profil, aucune action metier n'est possible, et un compte cree
 * puis abandonne au milieu du gue ne sert a personne.
 *
 * Trois appels s'enchainent : creation du compte, creation du profil,
 * puis depot de la photo. Si la photo echoue, le compte reste valide et
 * l'utilisateur peut la reprendre depuis son profil.
 */
@Component({
  selector: 'gs-student-register-page',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [ReactiveFormsModule, RouterLink, SessionNotice],
  templateUrl: './student-register-page.html',
})
export class StudentRegisterPage {
  private readonly fb = inject(FormBuilder);
  private readonly auth = inject(AuthService);
  private readonly users = inject(UserService);
  private readonly router = inject(Router);

  protected readonly etape = signal<1 | 2>(1);
  protected readonly enCours = signal(false);
  protected readonly erreur = signal<string | null>(null);

  protected readonly gouvernorats = signal<readonly Option[]>([]);
  protected readonly typesEtablissement = signal<readonly Option[]>([]);

  /** Photo choisie, et son apercu en memoire. */
  protected readonly photo = signal<File | null>(null);
  protected readonly apercu = signal<string | null>(null);

  /** Bac+1 à Bac+8. */
  protected readonly niveaux = [1, 2, 3, 4, 5, 6, 7, 8] as const;

  protected readonly identite = this.fb.nonNullable.group({
    firstName: ['', [Validators.required, Validators.maxLength(60)]],
    lastName: ['', [Validators.required, Validators.maxLength(60)]],
    cin: ['', [Validators.required, Validators.pattern('^[0-9]{8}$')]],
    phone: ['', [Validators.required, Validators.pattern('^[0-9+ ]{8,20}$')]],
    email: ['', [Validators.required, Validators.email]],
    password: ['', [Validators.required, Validators.minLength(8)]],
  });

  protected readonly scolarite = this.fb.nonNullable.group({
    institutionName: ['', [Validators.required, Validators.maxLength(150)]],
    institutionType: ['PUBLIQUE', Validators.required],
    academicLevel: [3, Validators.required],
    classe: ['', [Validators.required, Validators.maxLength(20)]],
    departement: ['', [Validators.required, Validators.maxLength(80)]],
    address: ['', [Validators.required, Validators.maxLength(255)]],
    city: ['', [Validators.required, Validators.maxLength(80)]],
    governorate: ['', Validators.required],
  });

  protected readonly initiales = computed(() => {
    const v = this.identite.getRawValue();
    const p = v.firstName.charAt(0);
    const n = v.lastName.charAt(0);
    return (p + n).toUpperCase();
  });

  constructor() {
    this.users.referentiels().subscribe({
      next: (r) => {
        this.gouvernorats.set(r.gouvernorats);
        this.typesEtablissement.set(r.typesEtablissement);
      },
      error: () => this.erreur.set('Impossible de charger la liste des gouvernorats.'),
    });
  }

  protected choisirPhoto(event: Event): void {
    const input = event.target as HTMLInputElement;
    const fichier = input.files?.[0] ?? null;
    if (!fichier) {
      return;
    }
    if (fichier.size > 2 * 1024 * 1024) {
      this.erreur.set('La photo ne doit pas dépasser 2 Mo.');
      input.value = '';
      return;
    }
    this.erreur.set(null);
    this.photo.set(fichier);

    const lecteur = new FileReader();
    lecteur.onload = () => this.apercu.set(lecteur.result as string);
    lecteur.readAsDataURL(fichier);
  }

  protected retirerPhoto(): void {
    this.photo.set(null);
    this.apercu.set(null);
  }

  protected suivant(): void {
    if (this.identite.invalid) {
      this.identite.markAllAsTouched();
      this.erreur.set('Complétez les champs obligatoires.');
      return;
    }
    this.erreur.set(null);
    this.etape.set(2);
    window.scrollTo({ top: 0, behavior: 'smooth' });
  }

  protected precedent(): void {
    this.erreur.set(null);
    this.etape.set(1);
  }

  protected soumettre(): void {
    if (this.scolarite.invalid || this.enCours()) {
      this.scolarite.markAllAsTouched();
      this.erreur.set('Complétez les champs obligatoires.');
      return;
    }

    this.enCours.set(true);
    this.erreur.set(null);

    const i = this.identite.getRawValue();

    // 1. Le compte
    this.auth
      .register({
        email: i.email.trim(),
        password: i.password,
        firstName: i.firstName.trim(),
        lastName: i.lastName.trim(),
        role: 'ETUDIANT',
      })
      .subscribe({
        next: () => this.creerProfil(),
        error: (e: { status?: number; error?: { message?: string } }) => {
          this.enCours.set(false);
          this.erreur.set(
            e.status === 409
              ? 'Un compte existe déjà avec cette adresse email.'
              : (e.error?.message ?? 'Création du compte impossible.'),
          );
          this.etape.set(1);
        },
      });
  }

  /** 2. Le profil de scolarite, une fois le compte cree et connecte. */
  private creerProfil(): void {
    const i = this.identite.getRawValue();
    const s = this.scolarite.getRawValue();

    this.users
      .saveMyStudentProfile({
        firstName: i.firstName.trim(),
        lastName: i.lastName.trim(),
        email: i.email.trim(),
        phone: i.phone.trim(),
        cin: i.cin.trim(),
        classe: s.classe.trim().toUpperCase(),
        departement: s.departement.trim(),
        institutionName: s.institutionName.trim(),
        institutionType: s.institutionType as 'PUBLIQUE' | 'PRIVEE',
        academicLevel: Number(s.academicLevel),
        address: s.address.trim(),
        city: s.city.trim(),
        governorate: s.governorate,
      })
      .subscribe({
        next: () => this.deposerPhoto(),
        error: (e: { error?: { message?: string; champs?: Record<string, string> } }) => {
          this.enCours.set(false);
          const champs = e.error?.champs;
          this.erreur.set(
            champs
              ? Object.values(champs).join(' · ')
              : (e.error?.message ??
                  'Compte créé, mais le profil n’a pas pu être enregistré. Complétez-le depuis « Mon profil ».'),
          );
        },
      });
  }

  /** 3. La photo, facultative : un echec ici ne compromet rien. */
  private deposerPhoto(): void {
    const fichier = this.photo();
    if (!fichier) {
      this.terminer();
      return;
    }
    this.users.uploadMyPhoto(fichier).subscribe({
      next: () => this.terminer(),
      error: () => this.terminer(),
    });
  }

  private terminer(): void {
    this.enCours.set(false);
    void this.router.navigateByUrl('/etudiant');
  }
}
