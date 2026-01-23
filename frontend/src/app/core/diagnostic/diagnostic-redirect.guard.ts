import { inject } from '@angular/core';
import { CanActivateFn, Router, UrlTree } from '@angular/router';
import { catchError, map, of, take } from 'rxjs';
import { DiagnosticService } from './diagnostic.service';

export const diagnosticRedirectGuard: CanActivateFn = (): any => {
  const diagnostic = inject(DiagnosticService);
  const router = inject(Router);

  return diagnostic.getStatus().pipe(
    take(1),
    map(status => {
      // if diagnostic is required  go to /diagnostic
      if (status.required) return router.createUrlTree(['/diagnostic']);
      return true;
    }),
    // if the call fails
    catchError(() => of(true))
  );
};
