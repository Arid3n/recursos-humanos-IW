import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { Router, UrlTree, provideRouter } from '@angular/router';
import { beforeEach, describe, expect, it } from 'vitest';

import { absenceRequestGuard } from './auth.guard';
import { AuthService } from '../services/auth.service';
import { UserRole } from '../models/auth.models';

describe('absenceRequestGuard (HU-05 FR-017)', () => {
  beforeEach(() => {
    TestBed.resetTestingModule();
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting(), provideRouter([])]
    });
  });

  function run(role: UserRole | null): boolean | UrlTree {
    TestBed.inject(AuthService).currentUser.set(
      role ? { id: 'u-1', username: 'usuario@empresa.com', role } : null
    );
    return TestBed.runInInjectionContext(() => absenceRequestGuard({} as never, {} as never)) as boolean | UrlTree;
  }

  it('redirects DIRECCION_GENERAL to the «Dirección General no solicita ausencias» page', () => {
    const result = run('DIRECCION_GENERAL');
    expect(result).toBeInstanceOf(UrlTree);
    expect(TestBed.inject(Router).serializeUrl(result as UrlTree)).toBe('/leave/not-available');
  });

  it('lets EMPLEADO, RESPONSABLE_EQUIPO and RRHH through', () => {
    for (const role of ['EMPLEADO', 'RESPONSABLE_EQUIPO', 'RRHH'] as UserRole[]) {
      expect(run(role)).toBe(true);
    }
  });

  it('sends an unauthenticated user to /login', () => {
    const result = run(null);
    expect(result).toBeInstanceOf(UrlTree);
    expect(TestBed.inject(Router).serializeUrl(result as UrlTree)).toBe('/login');
  });
});
