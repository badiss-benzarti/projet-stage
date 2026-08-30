import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';

import { Role, ROLE_LABELS } from '../../core/models/auth.models';
import { AuthService } from '../../core/services/auth.service';

/** Roles ouverts a l'inscription libre. */
const ROLES_OUVERTS: readonly Role[] = ['ETUDIANT', 'ENTREPRISE'];

/**
 * Formulaire de creation de compte.
 *
 * Le role vient de l'ecran de choix precedent. Un role non ouvert dans
 * l'URL est refuse : laisser quiconque s'attribuer un role de validation
 * viderait le workflow de son sens.
 */
@Component({
  selector: 'gs-register-page',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [ReactiveFormsModule, RouterLink],
  templateUrl: './register-page.html',
})
export class RegisterPage {
  private readonly fb = inject(FormBuilder);
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);
  private readonly route = inject(ActivatedRoute);

  protected readonly enCours = signal(false);
  protected readonly erreur = signal<string | null>(null);

  /** Role demande dans l'URL, null s'il n'est pas ouvert a l'inscription. */
  protected readonly role = signal<Role | null>(this.lireRole());

  protected readonly roleLabel = computed(() => {
    const r = this.role();
    return r ? ROLE_LABELS[r] : '';
  });

  protected readonly estEntreprise = computed(() => this.role() === 'ENTREPRISE');

  protected readonly form = this.fb.nonNullable.group({
    firstName: ['', [Validators.required, Validators.maxLength(60)]],
    lastName: ['', [Validators.required, Validators.maxLength(60)]],
    email: ['', [Validators.required, Validators.email]],
    password: ['', [Validators.required, Validators.minLength(8)]],
  });

  private lireRole(): Role | null {
    const brut = this.route.snapshot.paramMap.get('role') as Role | null;
    return brut && ROLES_OUVERTS.includes(brut) ? brut : null;
  }

  protected soumettre(): void {
    const role = this.role();
    if (!role || this.form.invalid || this.enCours()) {
      this.form.markAllAsTouched();
      return;
    }

    this.enCours.set(true);
    this.erreur.set(null);

    this.auth.register({ ...this.form.getRawValue(), role }).subscribe({
      next: () => {
        this.enCours.set(false);
        // Le compte est cree ET connecte : on enchaine sur le profil,
        // sans lequel aucune action metier n'est possible.
        void this.router.navigateByUrl(
          role === 'ENTREPRISE' ? '/entreprise/profil' : '/etudiant/profil',
        );
      },
      error: (e: { status?: number; error?: { message?: string; champs?: Record<string, string> } }) => {
        this.enCours.set(false);
        const champs = e.error?.champs;
        this.erreur.set(
          e.status === 409
            ? 'Un compte existe déjà avec cette adresse email.'
            : champs
              ? Object.values(champs).join(' · ')
              : (e.error?.message ?? 'Création du compte impossible.'),
        );
      },
    });
  }
}
