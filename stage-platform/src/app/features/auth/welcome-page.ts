import { ChangeDetectionStrategy, Component } from '@angular/core';
import { RouterLink } from '@angular/router';

/**
 * Porte d'entree de la plateforme.
 *
 * Deux chemins, un seul choix a faire : se connecter, ou creer un compte.
 * Tout le reste attend derriere.
 */
@Component({
  selector: 'gs-welcome-page',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [RouterLink],
  templateUrl: './welcome-page.html',
})
export class WelcomePage {}
