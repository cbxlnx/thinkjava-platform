import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { catchError, map, of, switchMap } from 'rxjs';
import { AuthService } from './auth.service';
import { DiagnosticService } from '../diagnostic/diagnostic.service';
import { UsersService } from '../users/users.service';

export const authGuard: CanActivateFn = (route, state) => {
  const auth = inject(AuthService);
  const router = inject(Router);
  const diagnostic = inject(DiagnosticService);
  const users = inject(UsersService);

  if (!auth.isLoggedIn()) return router.parseUrl('/login');

  // allow opening onboarding pages if logged in
  if (state.url.startsWith('/diagnostic') || state.url.startsWith('/welcome')) return true;

  // 1) enforce name
  return users.me().pipe(
    switchMap((me) => {
      const firstName = (me.firstName ?? '').trim();
      if (!firstName) return of(router.parseUrl('/welcome'));

      // 2) enforce diagnostic 
      return diagnostic.getStatus().pipe(
        map((s) => (s.required ? router.parseUrl('/diagnostic') : true)),
        catchError(() => of(router.parseUrl('/diagnostic')))
      );
    }),
    catchError(() => of(router.parseUrl('/welcome')))
  );
};