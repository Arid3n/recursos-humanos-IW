import { Component, DestroyRef, OnInit, computed, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { NavigationEnd, Router } from '@angular/router';
import { filter } from 'rxjs';
import { MatBadgeModule } from '@angular/material/badge';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatMenuModule } from '@angular/material/menu';

import { NotificationService } from '../../services/notification.service';
import { AppNotification } from '../../models/notification.models';

@Component({
  selector: 'app-notification-bell',
  standalone: true,
  imports: [CommonModule, MatBadgeModule, MatButtonModule, MatIconModule, MatMenuModule],
  templateUrl: './notification-bell.component.html',
  styleUrls: ['./notification-bell.component.scss']
})
export class NotificationBellComponent implements OnInit {
  private readonly notificationService = inject(NotificationService);
  private readonly router = inject(Router);
  private readonly destroyRef = inject(DestroyRef);

  readonly unreadCount = this.notificationService.unreadCount;
  readonly notifications = signal<AppNotification[]>([]);
  readonly loading = signal<boolean>(false);

  /** Texto accesible: el número no se transmite solo con el color del distintivo (Art. 21). */
  readonly ariaLabel = computed(() => {
    const count = this.unreadCount();
    return count === 1 ? '1 notificación sin leer' : `${count} notificaciones sin leer`;
  });

  ngOnInit(): void {
    this.notificationService.refreshUnreadCount();
    this.router.events
      .pipe(
        filter((event) => event instanceof NavigationEnd),
        takeUntilDestroyed(this.destroyRef)
      )
      .subscribe(() => this.notificationService.refreshUnreadCount());
  }

  loadNotifications(): void {
    this.loading.set(true);
    this.notificationService.getMyNotifications(0, 10).subscribe({
      next: (page) => {
        this.notifications.set(page.content ?? []);
        this.loading.set(false);
      },
      error: () => this.loading.set(false)
    });
  }

  onSelect(notification: AppNotification): void {
    if (notification.read) {
      return;
    }
    this.notificationService.markAsRead(notification.id).subscribe(() => {
      this.notifications.update((list) =>
        list.map((n) => (n.id === notification.id ? { ...n, read: true } : n))
      );
    });
  }
}
