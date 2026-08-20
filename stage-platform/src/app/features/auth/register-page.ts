import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';

import { Role } from '../../core/models/auth.models';
import { AuthService } from '../../core/services/auth.service';

/**
 * Creation de compte.
 *
 * Seuls ETUDIANT et ENTREPRISE peuvent s'inscrire librement. Les
 * encadrants sont declares par leur entreprise, et les responsables de
 * departement sont crees par l'administration : laisser quiconque
 * s'attribuer un role de validation viderait le workflow de son sens.
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

  protected readonly enCours = signal(false);
  protected readonly erreur = signal<string | null>(null);

  protected readonly roles: readonly { valeur: Role; libelle: string; aide: string }[] = [
    { valeur: 'ETUDIANT', libelle: 'Étudiant', aide: 'Déposer une demande et suivre mon stage' },
    { valeur: 'ENTREPRISE', libelle: 'Entreprise', aide: 'Accueillir des stagiaires' },
  ];

  protected readonly form = this.fb.nonNullable.group({
    role: ['ETUDIANT' as Role, Validators.required],
    firstName: ['', [Validators.required, Validators.maxLength(60)]],
    lastName: ['', [Validators.required, Validators.maxLength(60)]],
    email: ['', [Validators.required, Validators.email]],
    password: ['', [Validators.required, Validators.minLength(8)]],
  });

  protected soumettre(): void {
    if (this.form.invalid || this.enCours()) {
      this.form.markAllAsTouched();
      return;
    }

    this.enCours.set(true);
    this.erreur.set(null);

    this.auth.register(this.form.getRawValue()).subscribe({
      next: () => {
        this.enCours.set(false);
        // Le compte est cree ET connecte : on enchaine sur le profil,
        // sans lequel aucune action metier n'est possible.
        void this.router.navigateByUrl(
          this.form.controls.role.value === 'ENTREPRISE' ? '/entreprise/profil' : '/etudiant/profil',
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
