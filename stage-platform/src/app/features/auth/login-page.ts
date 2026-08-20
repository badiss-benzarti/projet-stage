import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';

import { AuthService } from '../../core/services/auth.service';

interface CompteDemo {
  readonly libelle: string;
  readonly email: string;
  readonly motDePasse: string;
}

@Component({
  selector: 'gs-login-page',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [ReactiveFormsModule, RouterLink],
  templateUrl: './login-page.html',
})
export class LoginPage {
  private readonly fb = inject(FormBuilder);
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);

  protected readonly enCours = signal(false);
  protected readonly erreur = signal<string | null>(null);

  protected readonly form = this.fb.nonNullable.group({
    email: ['', [Validators.required, Validators.email]],
    password: ['', [Validators.required]],
  });

  /** Les huit etats du dossier, affiches a gauche : c'est le sujet meme. */
  protected readonly etapes = [
    'Demande déposée',
    'Examen du service des stages',
    'Approbation',
    'Réponse de l’entreprise',
    'Stage en cours',
    'Journal et évaluation',
    'Note et réclamation',
    'Clôture',
  ] as const;

  /** Comptes de demonstration, retires avant la mise en production. */
  protected readonly comptes: readonly CompteDemo[] = [
    { libelle: 'Étudiant', email: 'ahmed.bensalah@esprit.tn', motDePasse: 'Etudiant@2026' },
    { libelle: 'Entreprise', email: 'soc.tech@partner.tn', motDePasse: 'Entreprise@2026' },
    { libelle: 'Encadrant', email: 'encadrant@partner.tn', motDePasse: 'Encadrant@2026' },
    { libelle: 'Dépt. stages', email: 'chef.stages@esprit.tn', motDePasse: 'ChefStage@2026' },
    { libelle: 'Dépt. pédagogique', email: 'chef.pedago@esprit.tn', motDePasse: 'ChefPedago@2026' },
  ];

  protected remplir(compte: CompteDemo): void {
    this.form.setValue({ email: compte.email, password: compte.motDePasse });
    this.erreur.set(null);
  }

  protected soumettre(): void {
    if (this.form.invalid || this.enCours()) {
      this.form.markAllAsTouched();
      return;
    }

    this.enCours.set(true);
    this.erreur.set(null);

    this.auth.login(this.form.getRawValue()).subscribe({
      next: () => {
        this.enCours.set(false);
        void this.router.navigateByUrl(this.auth.homeRoute());
      },
      error: (e: { status?: number }) => {
        this.enCours.set(false);
        this.erreur.set(
          e.status === 401
            ? 'Email ou mot de passe incorrect.'
            : 'Le service d’authentification est injoignable. Vérifiez que la passerelle tourne sur le port 8090.',
        );
      },
    });
  }
}
