import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { provideRouter } from '@angular/router';
import { beforeEach, describe, expect, it } from 'vitest';

import { AuthService } from './auth.service';
import { UserRole } from '../models/auth.models';

describe('AuthService (roles)', () => {
  beforeEach(() => {
    TestBed.resetTestingModule();
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting(), provideRouter([])]
    });
  });

  function serviceWithRole(role: UserRole | null): AuthService {
    const service = TestBed.inject(AuthService);
    service.currentUser.set(role ? { id: 'u-1', username: 'usuario@empresa.com', role } : null);
    return service;
  }

  it('isDireccionGeneral is true for the DIRECCION_GENERAL role', () => {
    expect(serviceWithRole('DIRECCION_GENERAL').isDireccionGeneral()).toBe(true);
  });

  it('isDireccionGeneral is false for the other roles and without session', () => {
    for (const role of ['EMPLEADO', 'RESPONSABLE_EQUIPO', 'RRHH', null] as (UserRole | null)[]) {
      expect(serviceWithRole(role).isDireccionGeneral()).toBe(false);
    }
  });

  it('a DIRECCION_GENERAL user is neither responsable, empleado nor RRHH', () => {
    const service = serviceWithRole('DIRECCION_GENERAL');
    expect(service.isResponsable()).toBe(false);
    expect(service.isEmpleado()).toBe(false);
    expect(service.isRRHH()).toBe(false);
  });

  // ---- HU-05 fase 10 (FR-018, FR-017): quién resuelve solicitudes ----

  it('FR-018: canResolveRequests is true for RESPONSABLE_EQUIPO, RRHH and DIRECCION_GENERAL', () => {
    for (const role of ['RESPONSABLE_EQUIPO', 'RRHH', 'DIRECCION_GENERAL'] as UserRole[]) {
      expect(serviceWithRole(role).canResolveRequests()).toBe(true);
    }
  });

  it('FR-018: canResolveRequests is false for EMPLEADO and without session', () => {
    for (const role of ['EMPLEADO', null] as (UserRole | null)[]) {
      expect(serviceWithRole(role).canResolveRequests()).toBe(false);
    }
  });
});
