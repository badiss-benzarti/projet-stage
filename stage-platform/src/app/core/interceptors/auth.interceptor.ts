import { HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { Router } from '@angular/router';
import { catchError, throwError } from 'rxjs';

import { AuthService } from '../services/auth.service';

/**
 * Ajoute le jeton a chaque appel et traite les deux refus differemment.
 *
 * 401 = "je ne sais pas qui tu es" -> deconnexion et retour au login.
 * 403 = "je sais qui tu es, mais tu n'as pas le droit" -> on reste sur
 * place, le composant affiche le message. C'est precisement pour rendre
 * cette distinction possible que le backend renvoie 401 et non 403 quand
 * le jeton manque.
 */
export const authInterceptor: HttpInterceptorFn = (req, next) => {
  const auth = inject(AuthService);
  const router = inject(Router);
  const token = auth.token();

  const requete = token
    ? req.clone({ setHeaders: { Authorization: `Bearer ${token}` } })
    : req;

  return next(requete).pipe(
    catchError((erreur: HttpErrorResponse) => {
      if (erreur.status === 401 && !req.url.includes('/api/auth/login')) {
        auth.logout();
        void router.navigate(['/connexion']);
      }
      return throwError(() => erreur);
    }),
  );
};
