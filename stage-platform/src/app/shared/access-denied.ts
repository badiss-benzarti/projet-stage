import { ChangeDetectionStrategy, Component, inject } from '@angular/core';
import { Router } from '@angular/router';

import { AuthService } from '../core/services/auth.service';

@Component({
  selector: 'gs-access-denied',
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <div class="flex min-h-screen items-center justify-center bg-sand-100 px-6">
      <div class="max-w-md text-center">
        <p class="eyebrow">Erreur 403</p>
        <h1 class="mt-2 text-2xl font-medium text-sand-900">Accès refusé</h1>
        <p class="mt-2 text-sm text-sand-500">
          Votre rôle ne donne pas accès à cette page. Si vous pensez qu’il s’agit
          d’une erreur, rapprochez-vous du service des stages.
        </p>
        <button
          type="button"
          (click)="retour()"
          class="mt-6 rounded-[5px] bg-petrol-700 px-4 py-2 text-sm font-medium text-white hover:bg-petrol-800"
        >
          Revenir à mon espace
        </button>
      </div>
    </div>
  `,
})
export class AccessDenied {
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);

  protected retour(): void {
    void this.router.navigateByUrl(this.auth.homeRoute());
  }
}
