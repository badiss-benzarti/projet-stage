import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router } from '@angular/router';

import { AuthService } from '../../core/services/auth.service';
import { Option, StudentProfile, UserService } from '../../core/services/user.service';
import { telechargerBlob } from '../../shared/download';
import { Spinner } from '../../shared/spinner';

/**
 * Profil de scolarite de l'etudiant.
 *
 * Obligatoire avant toute action metier : internship-service resout
 * l'etudiant par ce profil, pas par son compte.
 */
@Component({
  selector: 'gs-profile-page',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [ReactiveFormsModule, Spinner],
  templateUrl: './profile-page.html',
})
export class ProfilePage {
  private readonly fb = inject(FormBuilder);
  private readonly users = inject(UserService);
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);

  protected readonly chargement = signal(true);
  protected readonly envoi = signal(false);
  protected readonly erreur = signal<string | null>(null);
  protected readonly succes = signal<string | null>(null);
  protected readonly existe = signal(false);

  protected readonly profil = signal<StudentProfile | null>(null);
  protected readonly gouvernorats = signal<readonly Option[]>([]);
  protected readonly typesEtablissement = signal<readonly Option[]>([]);
  protected readonly niveaux = [1, 2, 3, 4, 5, 6, 7, 8] as const;

  /** Aperçu local d'une photo qui vient d'être choisie. */
  protected readonly apercu = signal<string | null>(null);
  protected readonly photoUrl = signal<string | null>(null);

  /** Depot ou retrait du CV en cours : evite le double clic. */
  protected readonly envoiCv = signal(false);

  protected readonly form = this.fb.nonNullable.group({
    firstName: ['', [Validators.required, Validators.maxLength(60)]],
    lastName: ['', [Validators.required, Validators.maxLength(60)]],
    email: ['', [Validators.required, Validators.email]],
    phone: [''],
    cin: ['', Validators.pattern('^$|^[0-9]{8}$')],
    institutionName: [''],
    institutionType: ['PUBLIQUE'],
    academicLevel: [3],
    classe: ['', [Validators.required, Validators.maxLength(20)]],
    departement: ['', [Validators.required, Validators.maxLength(80)]],
    address: [''],
    city: [''],
    governorate: [''],
  });

  constructor() {
    this.users.referentiels().subscribe({
      next: (r) => {
        this.gouvernorats.set(r.gouvernorats);
        this.typesEtablissement.set(r.typesEtablissement);
      },
      error: () => undefined,
    });

    const compte = this.auth.currentUser();

    this.users.myStudentProfile().subscribe({
      next: (p) => {
        this.existe.set(true);
        this.profil.set(p);
        if (p.hasPhoto) {
          this.chargerPhoto(p.id);
        }
        this.form.patchValue({
          firstName: p.firstName,
          lastName: p.lastName,
          email: p.email,
          phone: p.phone ?? '',
          cin: p.cin ?? '',
          institutionName: p.institutionName ?? '',
          institutionType: p.institutionType ?? 'PUBLIQUE',
          academicLevel: p.academicLevel ?? 3,
          classe: p.classe,
          departement: p.departement,
          address: p.address ?? '',
          city: p.city ?? '',
          governorate: p.governorate ?? '',
        });
        this.chargement.set(false);
      },
      // 404 : profil pas encore cree, on prefill depuis le compte.
      error: () => {
        this.existe.set(false);
        if (compte) {
          this.form.patchValue({
            firstName: compte.firstName,
            lastName: compte.lastName,
            email: compte.email,
          });
        }
        this.chargement.set(false);
      },
    });
  }

  protected changerPhoto(event: Event): void {
    const input = event.target as HTMLInputElement;
    const fichier = input.files?.[0];
    if (!fichier) {
      return;
    }
    if (!this.existe()) {
      this.erreur.set('Enregistrez d’abord votre profil, puis ajoutez votre photo.');
      input.value = '';
      return;
    }

    const lecteur = new FileReader();
    lecteur.onload = () => this.apercu.set(lecteur.result as string);
    lecteur.readAsDataURL(fichier);

    this.erreur.set(null);
    this.users.uploadMyPhoto(fichier).subscribe({
      next: (p) => {
        this.profil.set(p);
        this.chargerPhoto(p.id);
        this.succes.set('Photo mise à jour.');
        input.value = '';
      },
      error: (e: { error?: { message?: string } }) => {
        this.apercu.set(null);
        input.value = '';
        this.erreur.set(e.error?.message ?? 'Dépôt de la photo impossible.');
      },
    });
  }

  protected deposerCv(event: Event): void {
    const input = event.target as HTMLInputElement;
    const fichier = input.files?.[0];
    if (!fichier) {
      return;
    }
    if (!this.existe()) {
      this.erreur.set('Enregistrez d’abord votre profil, puis ajoutez votre CV.');
      input.value = '';
      return;
    }

    this.erreur.set(null);
    this.envoiCv.set(true);
    this.users.uploadMyCv(fichier).subscribe({
      next: (p) => {
        this.profil.set(p);
        this.envoiCv.set(false);
        this.succes.set('CV déposé.');
        input.value = '';
      },
      error: (e: { error?: { message?: string } }) => {
        this.envoiCv.set(false);
        input.value = '';
        this.erreur.set(e.error?.message ?? 'Dépôt du CV impossible.');
      },
    });
  }

  protected telechargerCv(): void {
    const p = this.profil();
    if (!p?.hasCv) {
      return;
    }
    this.users.myCvBlob().subscribe({
      next: (blob) => telechargerBlob(blob, p.cvName ?? 'cv.pdf'),
      error: () => this.erreur.set('Téléchargement du CV impossible.'),
    });
  }

  protected retirerCv(): void {
    if (!this.profil()?.hasCv) {
      return;
    }
    this.erreur.set(null);
    this.envoiCv.set(true);
    this.users.deleteMyCv().subscribe({
      next: (p) => {
        this.profil.set(p);
        this.envoiCv.set(false);
        this.succes.set('CV retiré.');
      },
      error: (e: { error?: { message?: string } }) => {
        this.envoiCv.set(false);
        this.erreur.set(e.error?.message ?? 'Suppression du CV impossible.');
      },
    });
  }

  /** Recupere la photo et la publie en URL objet, l'ancienne est liberee. */
  private chargerPhoto(studentId: number): void {
    this.users.photoBlob(studentId).subscribe({
      next: (blob) => {
        const ancienne = this.photoUrl();
        if (ancienne) {
          URL.revokeObjectURL(ancienne);
        }
        this.photoUrl.set(URL.createObjectURL(blob));
        this.apercu.set(null);
      },
      error: () => this.photoUrl.set(null),
    });
  }

  protected soumettre(): void {
    if (this.form.invalid || this.envoi()) {
      this.form.markAllAsTouched();
      return;
    }

    const v = this.form.getRawValue();
    const profil = {
      firstName: v.firstName.trim(),
      lastName: v.lastName.trim(),
      email: v.email.trim(),
      phone: v.phone.trim(),
      cin: v.cin.trim(),
      classe: v.classe.trim().toUpperCase(),
      departement: v.departement.trim(),
      institutionName: v.institutionName.trim(),
      institutionType: v.institutionType as 'PUBLIQUE' | 'PRIVEE',
      academicLevel: Number(v.academicLevel),
      address: v.address.trim(),
      city: v.city.trim(),
      governorate: v.governorate || null,
    };

    this.envoi.set(true);
    this.erreur.set(null);
    this.succes.set(null);

    const premier = !this.existe();
    const appel = premier
      ? this.users.saveMyStudentProfile(profil)
      : this.users.updateMyStudentProfile(profil);

    appel.subscribe({
      next: (p) => {
        this.existe.set(true);
        this.profil.set(p);
        this.envoi.set(false);
        this.succes.set('Profil enregistré.');
        if (premier) {
          setTimeout(() => void this.router.navigateByUrl('/etudiant'), 900);
        }
      },
      error: (e: { error?: { message?: string; champs?: Record<string, string> } }) => {
        this.envoi.set(false);
        const champs = e.error?.champs;
        this.erreur.set(
          champs
            ? Object.values(champs).join(' · ')
            : (e.error?.message ?? 'Enregistrement impossible.'),
        );
      },
    });
  }
}
