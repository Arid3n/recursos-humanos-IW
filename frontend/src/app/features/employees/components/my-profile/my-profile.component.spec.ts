import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideRouter } from '@angular/router';
import { beforeEach, describe, expect, it } from 'vitest';

import { MyProfileComponent } from './my-profile.component';
import { AuthService } from '../../../../core/services/auth.service';
import { UserRole } from '../../../../core/models/auth.models';

describe('MyProfileComponent (saldo por rol, HU-05 FR-017)', () => {
  const profile = {
    id: 'e-1',
    firstName: 'Elena',
    lastName: 'Vidal',
    email: 'elena@empresa.com',
    nationalId: '22222222J',
    hireDate: '2019-01-01',
    currentPosition: 'Directora',
    currentDepartment: 'Dirección',
    currentAgreementName: 'Convenio',
    managerFullName: null
  };

  async function render(role: UserRole): Promise<HTMLElement> {
    TestBed.resetTestingModule();
    await TestBed.configureTestingModule({
      imports: [MyProfileComponent],
      providers: [provideRouter([]), provideHttpClient(), provideHttpClientTesting()]
    }).compileComponents();
    TestBed.inject(AuthService).currentUser.set({ id: 'u-1', username: 'usuario@empresa.com', role });
    const fixture = TestBed.createComponent(MyProfileComponent);
    fixture.detectChanges();
    TestBed.inject(HttpTestingController).expectOne((r) => r.url.endsWith('/employees/me')).flush(profile);
    fixture.detectChanges();
    await fixture.whenStable();
    fixture.detectChanges();
    return fixture.nativeElement as HTMLElement;
  }

  beforeEach(() => TestBed.resetTestingModule());

  it('keeps the personal data but hides the balance widget for DIRECCION_GENERAL', async () => {
    const el = await render('DIRECCION_GENERAL');
    expect(el.textContent).toContain('Elena');
    expect(el.querySelector('app-leave-balance-widget')).toBeNull();
  });

  it('shows the balance widget for EMPLEADO, RESPONSABLE_EQUIPO and RRHH', async () => {
    for (const role of ['EMPLEADO', 'RESPONSABLE_EQUIPO', 'RRHH'] as UserRole[]) {
      const el = await render(role);
      expect(el.querySelector('app-leave-balance-widget'), role).not.toBeNull();
    }
  });
});
