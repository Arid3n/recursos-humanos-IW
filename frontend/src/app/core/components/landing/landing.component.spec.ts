import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { provideRouter } from '@angular/router';
import { describe, expect, it } from 'vitest';

import { LandingComponent } from './landing.component';
import { AuthService } from '../../services/auth.service';
import { UserRole } from '../../models/auth.models';

describe('LandingComponent (enlace del calendario por rol, HU-05 FR-018)', () => {
  async function render(role: UserRole): Promise<HTMLElement> {
    TestBed.resetTestingModule();
    await TestBed.configureTestingModule({
      imports: [LandingComponent],
      providers: [provideRouter([]), provideHttpClient(), provideHttpClientTesting()]
    }).compileComponents();
    TestBed.inject(AuthService).currentUser.set({ id: 'u-1', username: 'usuario@empresa.com', role });
    const fixture = TestBed.createComponent(LandingComponent);
    await fixture.whenStable();
    fixture.detectChanges();
    return fixture.nativeElement as HTMLElement;
  }

  it('links the team calendar for RESPONSABLE_EQUIPO, RRHH and DIRECCION_GENERAL', async () => {
    for (const role of ['RESPONSABLE_EQUIPO', 'RRHH', 'DIRECCION_GENERAL'] as UserRole[]) {
      const el = await render(role);
      expect(el.querySelector('a[href="/team/calendar"]'), role).not.toBeNull();
    }
  });

  it('does not link the team calendar for EMPLEADO', async () => {
    const el = await render('EMPLEADO');
    expect(el.querySelector('a[href="/team/calendar"]')).toBeNull();
  });
});
