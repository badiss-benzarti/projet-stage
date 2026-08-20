import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router } from '@angular/router';

import { AuthService } from '../../core/services/auth.service';
import { UserService } from '../../core/services/user.service';
import { Spinner } from '../../shared/spinner';

/**
 * Profil de scolarite de l'etudiant.
 *
 * Obligatoire avant toute action metier : internship-service resout
 * l'etudiant par ce profil, pas par son compte. Sans lui, le depot d'une
 * demande echoue avec un message explicite.
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

  protected readonly form = this.fb.nonNullable.group({
    firstName: ['', [Validators.required, Validators.maxLength(60)]],
    lastName: ['', [Validators.required, Validators.maxLength(60)]],
    email: ['', [Validators.required, Validators.email]],
    phone: [''],
    cin: ['', Validators.pattern('^$|^[0-9]{8}$')],
    classe: ['', [Validators.required, Validators.maxLength(20)]],
    departement: ['', [Validators.required, Validators.maxLength(80)]],
  });

  constructor() {
    const compte = this.auth.currentUser();

    this.users.myStudentProfile().subscribe({
      next: (p) => {
        this.existe.set(true);
        this.form.patchValue({
          firstName: p.firstName,
          lastName: p.lastName,
          email: p.email,
          phone: p.phone ?? '',
          cin: p.cin ?? '',
          classe: p.classe,
          departement: p.departement,
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
    };

    this.envoi.set(true);
    this.erreur.set(null);
    this.succes.set(null);

    const premier = !this.existe();
    const appel = premier
      ? this.users.saveMyStudentProfile(profil)
      : this.users.updateMyStudentProfile(profil);

    appel.subscribe({
      next: () => {
        this.existe.set(true);
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
