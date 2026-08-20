import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core';
import { RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';

import { ROLE_LABELS } from '../core/models/auth.models';
import { AuthService } from '../core/services/auth.service';
import { NotificationService } from '../core/services/notification.service';
import { NAV_BY_ROLE, NavSection } from './nav';

/**
 * Coquille de l'application : barre laterale, en-tete, zone de contenu.
 *
 * La navigation depend du role du porteur du jeton, lu depuis un signal :
 * changer d'utilisateur recompose le menu sans rechargement.
 */
@Component({
  selector: 'gs-shell',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [RouterOutlet, RouterLink, RouterLinkActive],
  templateUrl: './shell.html',
})
export class Shell {
  private readonly auth = inject(AuthService);
  protected readonly notifications = inject(NotificationService);

  protected readonly user = this.auth.currentUser;
  protected readonly initials = this.auth.initials;
  protected readonly fullName = this.auth.fullName;

  protected readonly roleLabel = computed(() => {
    const r = this.auth.role();
    return r ? ROLE_LABELS[r] : '';
  });

  protected readonly sections = computed<readonly NavSection[]>(() => {
    const r = this.auth.role();
    return r ? NAV_BY_ROLE[r] : [];
  });

  protected readonly menuOuvert = signal(false);
  protected readonly barreOuverte = signal(false);

  constructor() {
    this.notifications.refreshUnread();
  }

  protected basculerMenu(): void {
    this.menuOuvert.update((v) => !v);
  }

  protected basculerBarre(): void {
    this.barreOuverte.update((v) => !v);
  }

  protected fermerBarre(): void {
    this.barreOuverte.set(false);
  }

  protected deconnexion(): void {
    this.menuOuvert.set(false);
    this.auth.logout();
  }
}
