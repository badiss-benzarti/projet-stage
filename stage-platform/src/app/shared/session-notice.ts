import { ChangeDetectionStrategy, Component, computed, inject } from '@angular/core';

import { ROLE_LABELS } from '../core/models/auth.models';
import { AuthService } from '../core/services/auth.service';

/**
 * Avertit qu'une session est deja ouverte sur les pages d'inscription.
 *
 * Creer un compte connecte automatiquement au nouveau : sans ce message,
 * l'utilisateur se retrouverait deconnecte de son compte sans comprendre
 * pourquoi.
 */
@Component({
  selector: 'gs-session-notice',
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    @if (connecte()) {
      <div class="mb-5 rounded-[6px] border border-warn-500/25 bg-warn-50 px-4 py-3">
        <p class="text-sm text-warn-700">
          Vous êtes connecté comme <span class="font-medium">{{ nom() }}</span>
          ({{ role() }}). Créer un compte fermera cette session.
        </p>
        <div class="mt-2 flex flex-wrap gap-3">
          <button type="button" (click)="retourEspace()"
            class="text-sm font-medium text-warn-700 underline underline-offset-2">
            Revenir à mon espace
          </button>
          <button type="button" (click)="deconnexion()"
            class="text-sm text-sand-600 underline underline-offset-2 hover:text-sand-900">
            Me déconnecter maintenant
          </button>
        </div>
      </div>
    }
  `,
})
export class SessionNotice {
  private readonly auth = inject(AuthService);

  protected readonly connecte = this.auth.isAuthenticated;
  protected readonly nom = this.auth.fullName;
  protected readonly role = computed(() => {
    const r = this.auth.role();
    return r ? ROLE_LABELS[r] : '';
  });

  protected retourEspace(): void {
    window.location.href = this.auth.homeRoute();
  }

  protected deconnexion(): void {
    this.auth.logout();
  }
}
