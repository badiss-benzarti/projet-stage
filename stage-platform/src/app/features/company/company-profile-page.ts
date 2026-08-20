import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { HttpClient } from '@angular/common/http';
import { Router } from '@angular/router';

import { AuthService } from '../../core/services/auth.service';
import { Company, UserService } from '../../core/services/user.service';
import { Spinner } from '../../shared/spinner';

/**
 * Profil de l'entreprise d'accueil.
 *
 * Obligatoire avant d'accueillir un stagiaire : c'est ce profil qui
 * rattache les demandes et les encadrants a la structure.
 */
@Component({
  selector: 'gs-company-profile-page',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [ReactiveFormsModule, Spinner],
  templateUrl: './company-profile-page.html',
})
export class CompanyProfilePage {
  private readonly fb = inject(FormBuilder);
  private readonly users = inject(UserService);
  private readonly auth = inject(AuthService);
  private readonly http = inject(HttpClient);
  private readonly router = inject(Router);

  protected readonly chargement = signal(true);
  protected readonly envoi = signal(false);
  protected readonly erreur = signal<string | null>(null);
  protected readonly succes = signal<string | null>(null);
  protected readonly existe = signal(false);

  protected readonly form = this.fb.nonNullable.group({
    name: ['', [Validators.required, Validators.maxLength(150)]],
    address: ['', [Validators.required, Validators.maxLength(255)]],
    phone: ['', Validators.required],
    email: ['', [Validators.required, Validators.email]],
    taxId: [''],
  });

  constructor() {
    const compte = this.auth.currentUser();

    this.users.myCompany().subscribe({
      next: (c) => {
        this.existe.set(true);
        this.form.patchValue({
          name: c.name,
          address: c.address,
          phone: c.phone,
          email: c.email,
          taxId: c.taxId ?? '',
        });
        this.chargement.set(false);
      },
      error: () => {
        this.existe.set(false);
        if (compte) {
          this.form.patchValue({ email: compte.email });
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
      name: v.name.trim(),
      address: v.address.trim(),
      phone: v.phone.trim(),
      email: v.email.trim(),
      taxId: v.taxId.trim(),
    };

    this.envoi.set(true);
    this.erreur.set(null);
    this.succes.set(null);

    const premier = !this.existe();
    const appel = premier
      ? this.http.post<Company>('/api/users/companies/me', profil)
      : this.http.put<Company>('/api/users/companies/me', profil);

    appel.subscribe({
      next: () => {
        this.existe.set(true);
        this.envoi.set(false);
        this.succes.set('Profil enregistré.');
        if (premier) {
          setTimeout(() => void this.router.navigateByUrl('/entreprise'), 900);
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
