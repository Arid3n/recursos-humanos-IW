import { Routes } from '@angular/router';
import { authGuard } from './core/guards/auth.guard';

export const routes: Routes = [
  {
    path: '',
    pathMatch: 'full',
    loadComponent: () => import('./core/components/landing/landing.component').then(m => m.LandingComponent)
  },
  {
    path: 'login',
    loadComponent: () => import('./core/components/login/login.component').then(m => m.LoginComponent)
  },
  {
    path: 'employees/new',
    canActivate: [authGuard],
    loadComponent: () => import('./employees/components/employee-create/employee-create.component').then(m => m.EmployeeCreateComponent)
  },
  {
    path: 'profile',
    canActivate: [authGuard],
    loadComponent: () => import('./features/employees/components/my-profile/my-profile.component').then(m => m.MyProfileComponent)
  },
  {
    path: 'leave/request',
    canActivate: [authGuard],
    loadComponent: () => import('./features/leave/components/leave-request-create/leave-request-create.component').then(m => m.LeaveRequestCreateComponent)
  },
  {
    path: 'leave/my-requests',
    canActivate: [authGuard],
    loadComponent: () => import('./features/leave/components/leave-request-list/leave-request-list.component').then(m => m.LeaveRequestListComponent)
  },
  {
    path: 'vacations',
    redirectTo: 'profile'
  },
  {
    path: '**',
    redirectTo: ''
  }
];
