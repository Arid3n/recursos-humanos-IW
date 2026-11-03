import { Injectable, inject, signal } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable, tap } from 'rxjs';
import { AppNotification, UnreadCount } from '../models/notification.models';
import { PageResponse } from '../../features/leave/models/leave.models';

@Injectable({
  providedIn: 'root'
})
export class NotificationService {
  private readonly http = inject(HttpClient);
  private readonly apiUrl = '/api/v1/notifications';

  /** Número de notificaciones no leídas del empleado en sesión (HU-05). */
  readonly unreadCount = signal<number>(0);

  getMyNotifications(page: number = 0, size: number = 10): Observable<PageResponse<AppNotification>> {
    const params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString());
    return this.http.get<PageResponse<AppNotification>>(this.apiUrl, { params });
  }

  refreshUnreadCount(): void {
    this.http.get<UnreadCount>(`${this.apiUrl}/unread-count`).subscribe({
      next: (res) => this.unreadCount.set(res.count),
      error: () => {
        // El contador es informativo: un fallo puntual no debe interrumpir la navegación
      }
    });
  }

  markAsRead(id: string): Observable<void> {
    return this.http.post<void>(`${this.apiUrl}/${id}/read`, null).pipe(
      tap(() => this.unreadCount.update((count) => Math.max(0, count - 1)))
    );
  }
}
