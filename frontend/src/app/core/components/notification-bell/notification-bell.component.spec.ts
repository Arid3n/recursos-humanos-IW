import { TestBed } from '@angular/core/testing';
import { signal } from '@angular/core';
import { provideRouter } from '@angular/router';
import { of } from 'rxjs';
import { describe, it, expect, beforeEach, vi } from 'vitest';

import { NotificationBellComponent } from './notification-bell.component';
import { NotificationService } from '../../services/notification.service';
import { AppNotification } from '../../models/notification.models';

describe('NotificationBellComponent', () => {
  const unread: AppNotification = {
    id: 'n-1',
    title: 'Solicitud rechazada',
    message: 'Tu solicitud de vacaciones del 01/12/2026 al 05/12/2026 ha sido rechazada. Motivo: Entrega del proyecto',
    read: false,
    createdAt: '2026-11-03T10:00:00Z'
  };
  const alreadyRead: AppNotification = { ...unread, id: 'n-2', title: 'Solicitud aprobada', read: true };

  let unreadCount = signal(2);
  let mockService: {
    unreadCount: typeof unreadCount;
    refreshUnreadCount: ReturnType<typeof vi.fn>;
    getMyNotifications: ReturnType<typeof vi.fn>;
    markAsRead: ReturnType<typeof vi.fn>;
  };

  beforeEach(async () => {
    unreadCount = signal(2);
    mockService = {
      unreadCount,
      refreshUnreadCount: vi.fn(),
      getMyNotifications: vi.fn().mockReturnValue(of({
        content: [unread, alreadyRead], page: 0, size: 10, totalElements: 2, totalPages: 1, first: true, last: true
      })),
      markAsRead: vi.fn().mockImplementation(() => {
        unreadCount.update((c) => c - 1);
        return of(undefined);
      })
    };

    await TestBed.configureTestingModule({
      imports: [NotificationBellComponent],
      providers: [
        provideRouter([]),
        { provide: NotificationService, useValue: mockService }
      ]
    }).compileComponents();
  });

  it('should refresh the unread count on init and expose an accessible label (HU-05.CA-4)', () => {
    const fixture = TestBed.createComponent(NotificationBellComponent);
    fixture.detectChanges();

    expect(mockService.refreshUnreadCount).toHaveBeenCalled();
    const button = (fixture.nativeElement as HTMLElement).querySelector('button');
    expect(button?.getAttribute('aria-label')).toBe('2 notificaciones sin leer');
  });

  it('should load the most recent notifications when the menu opens (HU-05.CA-4)', () => {
    const fixture = TestBed.createComponent(NotificationBellComponent);
    fixture.detectChanges();

    fixture.componentInstance.loadNotifications();

    expect(mockService.getMyNotifications).toHaveBeenCalledWith(0, 10);
    expect(fixture.componentInstance.notifications().length).toBe(2);
  });

  it('should mark an unread notification as read and decrement the counter (HU-05.CA-4)', () => {
    const fixture = TestBed.createComponent(NotificationBellComponent);
    fixture.detectChanges();
    fixture.componentInstance.loadNotifications();

    fixture.componentInstance.onSelect(unread);
    fixture.detectChanges();

    expect(mockService.markAsRead).toHaveBeenCalledWith('n-1');
    expect(fixture.componentInstance.notifications().find((n) => n.id === 'n-1')?.read).toBe(true);
    const button = (fixture.nativeElement as HTMLElement).querySelector('button');
    expect(button?.getAttribute('aria-label')).toBe('1 notificación sin leer');
  });

  it('should not call the API when selecting an already read notification', () => {
    const fixture = TestBed.createComponent(NotificationBellComponent);
    fixture.detectChanges();

    fixture.componentInstance.onSelect(alreadyRead);

    expect(mockService.markAsRead).not.toHaveBeenCalled();
  });
});
