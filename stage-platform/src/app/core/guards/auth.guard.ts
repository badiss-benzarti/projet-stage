import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';

import { Role } from '../models/auth.models';
import { AuthService } from '../services/auth.service';

/** Exige une session valide. */
export const authGuard: CanActivateFn = (_route, state) => {
  const auth = inject(AuthService);
  const router = inject(Router);

  if (auth.isAuthenticated()) {
    return true;
  }
  return router.createUrlTree(['/connexion'], {
    queryParams: { retour: state.url },
  });
};

/**
 * Exige un role precis.
 *
 * Ce garde ne fait que masquer une page : la vraie autorisation est
 * appliquee par le backend, qui renvoie 403 quoi qu'affiche le frontend.
 */
export const roleGuard = (...roles: readonly Role[]): CanActivateFn => {
  return () => {
    const auth = inject(AuthService);
    const router = inject(Router);

    if (!auth.isAuthenticated()) {
      return router.createUrlTree(['/connexion']);
    }
    return auth.hasAnyRole(...roles) ? true : router.createUrlTree(['/acces-refuse']);
  };
};
