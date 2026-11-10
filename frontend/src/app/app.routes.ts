import { Routes } from '@angular/router';
import { authGuard, performanceReviewGuard, rrhhGuard } from './core/guards/auth.guard';

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
    path: 'reports',
    canActivate: [rrhhGuard],
    loadComponent: () => import('./features/reports/components/department-reports/department-reports.component').then(m => m.DepartmentReportsComponent)
  },
  {
    path: 'performance-reviews',
    canActivate: [performanceReviewGuard],
    loadComponent: () => import('./features/performance/components/performance-reviews/performance-reviews.component').then(m => m.PerformanceReviewsComponent)
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
    path: 'team/requests',
    canActivate: [authGuard],
    loadComponent: () => import('./features/team-calendar/components/team-pending-requests/team-pending-requests.component').then(m => m.TeamPendingRequestsComponent)
  },
  {
    path: 'team/calendar',
    canActivate: [authGuard],
    loadComponent: () => import('./features/team-calendar/components/team-calendar/team-calendar.component').then(m => m.TeamCalendarComponent)
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
