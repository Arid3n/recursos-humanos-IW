import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { AuthService } from '../services/auth.service';

export const authGuard: CanActivateFn = () => {
  const authService = inject(AuthService);
  const router = inject(Router);

  if (authService.isAuthenticated()) {
    return true;
  }

  router.navigate(['/login']);
  return false;
};

export const rrhhGuard: CanActivateFn = () => {
  const authService = inject(AuthService);
  const router = inject(Router);

  if (!authService.isAuthenticated()) {
    router.navigate(['/login']);
    return false;
  }

  if (authService.isRRHH()) {
    return true;
  }

  // Not RRHH
  alert('Acceso restringido: Se requiere rol de Recursos Humanos (RRHH).');
  return false;
};

export const performanceReviewGuard: CanActivateFn = () => {
  const authService = inject(AuthService);
  const router = inject(Router);

  if (!authService.isAuthenticated()) {
    return router.createUrlTree(['/login']);
  }
  if (authService.isEmpleado() || authService.isResponsable()) {
    return true;
  }
  return router.createUrlTree(['/']);
};

/**
 * Rutas de solicitud de ausencia (HU-05 FR-017): Dirección General no solicita ausencias y se le
 * redirige a /leave/not-available. TODO T077: esqueleto para la fase roja de los tests.
 */
export const absenceRequestGuard: CanActivateFn = () => true;
