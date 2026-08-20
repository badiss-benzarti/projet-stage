import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject, signal } from '@angular/core';
import { Observable, tap } from 'rxjs';

import { Page } from '../models/internship.models';

export interface AppNotification {
  readonly id: number;
  readonly eventType: string;
  readonly title: string;
  readonly message: string;
  readonly internshipId: number | null;
  readonly read: boolean;
  readonly createdAt: string;
}

/**
 * Notifications issues du bus RabbitMQ.
 *
 * Le compteur non lu est un signal partage : la pastille de l'en-tete et
 * la page de notifications lisent la meme valeur, sans se coordonner.
 */
@Injectable({ providedIn: 'root' })
export class NotificationService {
  private readonly http = inject(HttpClient);
  private readonly base = '/api/notifications';

  private readonly _unread = signal(0);
  readonly unread = this._unread.asReadonly();

  refreshUnread(): void {
    this.http
      .get<{ unread: number }>(`${this.base}/unread-count`)
      .subscribe({
        next: (r) => this._unread.set(r.unread),
        // Un compteur indisponible ne doit pas polluer l'interface.
        error: () => this._unread.set(0),
      });
  }

  list(page = 0, size = 20): Observable<Page<AppNotification>> {
    return this.http.get<Page<AppNotification>>(`${this.base}/mine`, {
      params: new HttpParams().set('page', page).set('size', size),
    });
  }

  markRead(id: number): Observable<AppNotification> {
    return this.http
      .patch<AppNotification>(`${this.base}/${id}/read`, {})
      .pipe(tap(() => this._unread.update((n) => Math.max(0, n - 1))));
  }

  markAllRead(): Observable<{ updated: number }> {
    return this.http
      .post<{ updated: number }>(`${this.base}/read-all`, {})
      .pipe(tap(() => this._unread.set(0)));
  }
}
