import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { catchError, map, of } from 'rxjs';
import { AuthService } from './auth.service';
import { DiagnosticService } from '../diagnostic/diagnostic.service';

export const authGuard: CanActivateFn = (route, state) => {
  const auth = inject(AuthService);
  const router = inject(Router);
  const diagnostic = inject(DiagnosticService);

  // not logged in => go login
  if (!auth.isLoggedIn()) {
    return router.parseUrl('/login');
  }

  // always allow opening diagnostic page if logged in (otherwise can deadlock)
  if (state.url.startsWith('/diagnostic')) {
    return true;
  }

  // for any other protected route (dashboard etc), require diagnostic completion
  return diagnostic.getStatus().pipe(
    map((s) => {
      if (s.required) return router.parseUrl('/diagnostic');
      return true;
    }),
    catchError(() => of(router.parseUrl('/diagnostic')))
  );
};
