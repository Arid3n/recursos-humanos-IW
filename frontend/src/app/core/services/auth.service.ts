import { Injectable, computed, inject, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Router } from '@angular/router';
import { Observable, tap } from 'rxjs';
import { CurrentUser, LoginRequest, LoginResponse } from '../models/auth.models';

const STORAGE_KEY_USER = 'rrhh_auth_user';
const STORAGE_KEY_TOKEN = 'rrhh_auth_token';

@Injectable({
  providedIn: 'root'
})
export class AuthService {
  private readonly http = inject(HttpClient);
  private readonly router = inject(Router);
  private readonly authUrl = '/api/v1/auth';

  readonly currentUser = signal<CurrentUser | null>(this.loadStoredUser());
  readonly isAuthenticated = computed(() => !!this.currentUser());
  readonly userRole = computed(() => this.currentUser()?.role ?? null);
  readonly isRRHH = computed(() => this.userRole() === 'RRHH');
  readonly isResponsable = computed(() => this.userRole() === 'RESPONSABLE_EQUIPO');
  readonly isEmpleado = computed(() => this.userRole() === 'EMPLEADO');
  readonly isDireccionGeneral = computed(() => this.userRole() === 'DIRECCION_GENERAL');
  /** Quien puede ver la bandeja y el calendario de sus subordinados directos (HU-05 FR-017, FR-018). */
  readonly canResolveRequests = computed(() => {
    const role = this.userRole();
    return role === 'RESPONSABLE_EQUIPO' || role === 'RRHH' || role === 'DIRECCION_GENERAL';
  });

  login(credentials: LoginRequest): Observable<LoginResponse> {
    return this.http.post<LoginResponse>(`${this.authUrl}/login`, credentials).pipe(
      tap((res) => {
        const user: CurrentUser = {
          id: res.id,
          username: res.username,
          role: res.role
        };
        localStorage.setItem(STORAGE_KEY_TOKEN, res.token);
        localStorage.setItem(STORAGE_KEY_USER, JSON.stringify(user));
        this.currentUser.set(user);
      })
    );
  }

  logout(): void {
    localStorage.removeItem(STORAGE_KEY_TOKEN);
    localStorage.removeItem(STORAGE_KEY_USER);
    this.currentUser.set(null);
    this.router.navigate(['/login']);
  }

  getAuthToken(): string | null {
    return localStorage.getItem(STORAGE_KEY_TOKEN);
  }

  private loadStoredUser(): CurrentUser | null {
    try {
      const stored = localStorage.getItem(STORAGE_KEY_USER);
      return stored ? JSON.parse(stored) : null;
    } catch {
      return null;
    }
  }
}
