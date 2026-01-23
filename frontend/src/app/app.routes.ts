import { Routes } from '@angular/router';
import { LoginPageComponent } from './pages/login-page/login-page.component';
import { RegisterPageComponent } from './pages/register-page/register-page.component';
import { DashboardPageComponent } from './pages/dashboard-page/dashboard-page.component';
import { DiagnosticPageComponent } from './pages/diagnostic-page/diagnostic-page.component';
import { authGuard } from './core/auth/auth.guard';
import { diagnosticRedirectGuard } from './core/diagnostic/diagnostic-redirect.guard';

export const routes: Routes = [
  { path: 'login', component: LoginPageComponent },
  { path: 'register', component: RegisterPageComponent },

  // diagnostic page should only require auth
  { path: 'diagnostic', component: DiagnosticPageComponent, canActivate: [authGuard] },

  // dashboard requires auth and the diagnostic redirect guard
  { path: 'dashboard', component: DashboardPageComponent, canActivate: [authGuard, diagnosticRedirectGuard] },

  { path: '', pathMatch: 'full', redirectTo: 'dashboard' },
  { path: '**', redirectTo: 'dashboard' },
];
