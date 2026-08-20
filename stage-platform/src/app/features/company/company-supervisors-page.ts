import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { HttpClient } from '@angular/common/http';

import { Company, Supervisor, UserService } from '../../core/services/user.service';
import { EmptyState } from '../../shared/empty-state';
import { Spinner } from '../../shared/spinner';

/**
 * Declaration des encadrants de l'entreprise.
 *
 * Un encadrant doit d'abord posseder un compte ENCADRANT : c'est
 * l'auth-service qui cree le compte, user-service ne fait que lui
 * rattacher un profil et une entreprise. Le formulaire enchaine les deux.
 */
@Component({
  selector: 'gs-company-supervisors-page',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [ReactiveFormsModule, EmptyState, Spinner],
  templateUrl: './company-supervisors-page.html',
})
export class CompanySupervisorsPage {
  private readonly fb = inject(FormBuilder);
  private readonly users = inject(UserService);
  private readonly http = inject(HttpClient);

  protected readonly chargement = signal(true);
  protected readonly envoi = signal(false);
  protected readonly erreur = signal<string | null>(null);
  protected readonly succes = signal<string | null>(null);
  protected readonly formulaireOuvert = signal(false);

  protected readonly entreprise = signal<Company | null>(null);
  protected readonly encadrants = signal<readonly Supervisor[]>([]);

  protected readonly form = this.fb.nonNullable.group({
    firstName: ['', Validators.required],
    lastName: ['', Validators.required],
    email: ['', [Validators.required, Validators.email]],
    password: ['', [Validators.required, Validators.minLength(8)]],
    phone: [''],
    position: [''],
  });

  constructor() {
    this.charger();
  }

  private charger(): void {
    this.users.myCompany().subscribe({
      next: (c) => {
        this.entreprise.set(c);
        this.users.supervisorsOf(c.id).subscribe({
          next: (liste) => {
            this.encadrants.set(liste);
            this.chargement.set(false);
          },
          error: () => this.chargement.set(false),
        });
      },
      error: () => {
        this.chargement.set(false);
        this.erreur.set('Complétez d’abord le profil de votre entreprise.');
      },
    });
  }

  protected basculer(): void {
    this.formulaireOuvert.update((v) => !v);
    this.erreur.set(null);
    this.succes.set(null);
  }

  protected ajouter(): void {
    if (this.form.invalid || this.envoi()) {
      this.form.markAllAsTouched();
      return;
    }
    const v = this.form.getRawValue();
    this.envoi.set(true);
    this.erreur.set(null);
    this.succes.set(null);

    // 1. Creation du compte ENCADRANT
    this.http
      .post<{ user: { id: number } }>('/api/auth/register', {
        email: v.email.trim(),
        password: v.password,
        firstName: v.firstName.trim(),
        lastName: v.lastName.trim(),
        role: 'ENCADRANT',
      })
      .subscribe({
        next: (reponse) => this.rattacher(reponse.user.id, v),
        error: (e: { status?: number; error?: { message?: string } }) => {
          this.envoi.set(false);
          this.erreur.set(
            e.status === 409
              ? 'Un compte existe déjà avec cet email.'
              : (e.error?.message ?? 'Création du compte impossible.'),
          );
        },
      });
  }

  /** 2. Rattachement du profil encadrant a l'entreprise. */
  private rattacher(
    userId: number,
    v: { firstName: string; lastName: string; email: string; phone: string; position: string },
  ): void {
    this.http
      .post<Supervisor>('/api/users/supervisors', {
        userId,
        firstName: v.firstName.trim(),
        lastName: v.lastName.trim(),
        email: v.email.trim(),
        phone: v.phone.trim(),
        position: v.position.trim(),
      })
      .subscribe({
        next: (encadrant) => {
          this.encadrants.update((liste) => [...liste, encadrant]);
          this.form.reset();
          this.formulaireOuvert.set(false);
          this.envoi.set(false);
          this.succes.set(`${encadrant.firstName} ${encadrant.lastName} peut désormais se connecter.`);
        },
        error: (e: { error?: { message?: string } }) => {
          this.envoi.set(false);
          this.erreur.set(e.error?.message ?? 'Rattachement impossible.');
        },
      });
  }

  protected retirer(encadrant: Supervisor): void {
    this.http.delete<void>(`/api/users/supervisors/${encadrant.id}`).subscribe({
      next: () => this.encadrants.update((l) => l.filter((e) => e.id !== encadrant.id)),
      error: (e: { error?: { message?: string } }) =>
        this.erreur.set(e.error?.message ?? 'Suppression impossible.'),
    });
  }
}
