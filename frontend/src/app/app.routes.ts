import { Routes } from '@angular/router';
import { AppLayoutComponent } from './layout/app-layout/app-layout.component';
import { authGuard } from './core/auth/auth.guard';

export const routes: Routes = [
  // AUTH / DIAGNOSTIC (NO NAVBAR)
  {
    path: 'login',
    loadComponent: () => import('./pages/login-page/login-page.component')
      .then(m => m.LoginPageComponent),
  },
  {
    path: 'register',
    loadComponent: () => import('./pages/register-page/register-page.component')
      .then(m => m.RegisterPageComponent),
  },
  {
    path: 'diagnostic',
    loadComponent: () => import('./pages/diagnostic-page/diagnostic-page.component')
      .then(m => m.DiagnosticPageComponent),
  },
  {
    path: 'welcome',
    loadComponent: () => import('./pages/welcome-page/welcome-page.component')
        .then(m => m.WelcomePageComponent),
  },

  // APP (WITH NAVBAR) - PROTECTED
  {
    path: '',
    component: AppLayoutComponent,
    canActivateChild: [authGuard],  
    children: [
      {
        path: 'dashboard',
        loadComponent: () => import('./pages/dashboard-page/dashboard-page.component')
          .then(m => m.DashboardPageComponent),
      },
      {
        path: 'learn',
        loadComponent: () => import('./pages/learn-index-page/learn-index-page.component')
          .then(m => m.LearnIndexPageComponent),
      },
      {
        path: 'learn/lesson/:id',
        loadComponent: () => import('./pages/learn-lesson-page/learn-lesson-page.component')
          .then(m => m.LearnLessonPageComponent),
      },
      {
        path: '',
        redirectTo: 'dashboard',
        pathMatch: 'full',
      },
    ],
  },
];