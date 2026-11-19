import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { provideRouter } from '@angular/router';
import { AuthService } from './core/services/auth.service';
import { App } from './app';

describe('App', () => {
  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [App],
      providers: [provideRouter([])]
    }).compileComponents();
  });

  it('should create the app', () => {
    const fixture = TestBed.createComponent(App);
    const app = fixture.componentInstance;
    expect(app).toBeTruthy();
  });

  it('should render header brand title', async () => {
    const fixture = TestBed.createComponent(App);
    await fixture.whenStable();
    const compiled = fixture.nativeElement as HTMLElement;
    expect(compiled.querySelector('h1')?.textContent).toContain('Gestión de Recursos Humanos');
  });

  // ---- HU-05 fase 9 (FR-017): menú por rol ----

  describe('navigation by role', () => {
    async function menuFor(role: string): Promise<string> {
      TestBed.resetTestingModule();
      await TestBed.configureTestingModule({
        imports: [App],
        providers: [provideRouter([]), provideHttpClient(), provideHttpClientTesting()]
      }).compileComponents();
      TestBed.inject(AuthService).currentUser.set({
        id: 'u-1',
        username: 'usuario@empresa.com',
        role: role as 'EMPLEADO'
      });
      const fixture = TestBed.createComponent(App);
      await fixture.whenStable();
      fixture.detectChanges();
      return (fixture.nativeElement as HTMLElement).querySelector('.nav-links')?.textContent ?? '';
    }

    it('hides «Solicitar Ausencia» and «Mis Solicitudes» for DIRECCION_GENERAL and shows the team links', async () => {
      const menu = await menuFor('DIRECCION_GENERAL');
      expect(menu).not.toContain('Solicitar Ausencia');
      expect(menu).not.toContain('Mis Solicitudes');
      expect(menu).toContain('Solicitudes del Equipo');
      expect(menu).toContain('Calendario del Equipo');
      expect(menu).toContain('Dirección General');
    });

    it('FR-017: shows «Mi Ficha» without «y Saldo» for DIRECCION_GENERAL', async () => {
      const menu = await menuFor('DIRECCION_GENERAL');
      expect(menu).toContain('Mi Ficha');
      expect(menu).not.toContain('Mi Ficha y Saldo');
    });

    it('FR-018: shows the team, absence and HR links for RRHH', async () => {
      const menu = await menuFor('RRHH');
      expect(menu).toContain('Solicitudes del Equipo');
      expect(menu).toContain('Calendario del Equipo');
      expect(menu).toContain('Solicitar Ausencia');
      expect(menu).toContain('Mis Solicitudes');
      expect(menu).toContain('Alta de Empleado');
      expect(menu).toContain('Informes');
      expect(menu).toContain('Mi Ficha y Saldo');
    });

    it('keeps «Mi Ficha y Saldo» for EMPLEADO and RESPONSABLE_EQUIPO', async () => {
      for (const role of ['EMPLEADO', 'RESPONSABLE_EQUIPO']) {
        expect(await menuFor(role)).toContain('Mi Ficha y Saldo');
      }
    });

    it('keeps the absence and team links for RESPONSABLE_EQUIPO', async () => {
      const menu = await menuFor('RESPONSABLE_EQUIPO');
      expect(menu).toContain('Solicitar Ausencia');
      expect(menu).toContain('Mis Solicitudes');
      expect(menu).toContain('Solicitudes del Equipo');
    });

    it('shows the absence links but not the team links for EMPLEADO', async () => {
      const menu = await menuFor('EMPLEADO');
      expect(menu).toContain('Solicitar Ausencia');
      expect(menu).not.toContain('Solicitudes del Equipo');
      expect(menu).not.toContain('Calendario del Equipo');
    });
  });
});
