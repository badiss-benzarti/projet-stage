import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { DatePipe } from '@angular/common';

import { AppNotification, NotificationService } from '../../core/services/notification.service';
import { EmptyState } from '../../shared/empty-state';
import { Spinner } from '../../shared/spinner';

@Component({
  selector: 'gs-notifications-page',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [DatePipe, EmptyState, Spinner],
  templateUrl: './notifications-page.html',
})
export class NotificationsPage {
  protected readonly service = inject(NotificationService);

  protected readonly chargement = signal(true);
  protected readonly items = signal<readonly AppNotification[]>([]);

  constructor() {
    this.charger();
  }

  protected charger(): void {
    this.chargement.set(true);
    this.service.list(0, 50).subscribe({
      next: (page) => {
        this.items.set(page.content);
        this.chargement.set(false);
        this.service.refreshUnread();
      },
      error: () => {
        this.items.set([]);
        this.chargement.set(false);
      },
    });
  }

  protected marquerLue(n: AppNotification): void {
    if (n.read) {
      return;
    }
    this.service.markRead(n.id).subscribe({
      next: (maj) => this.items.update((liste) => liste.map((x) => (x.id === maj.id ? maj : x))),
    });
  }

  protected toutMarquerLu(): void {
    this.service.markAllRead().subscribe({
      next: () => this.items.update((liste) => liste.map((n) => ({ ...n, read: true }))),
    });
  }
}
