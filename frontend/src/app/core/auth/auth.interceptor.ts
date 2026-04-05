import { HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { AuthService } from './auth.service';

export const authInterceptor: HttpInterceptorFn = (req, next) => {
  const auth = inject(AuthService);
  const token = auth.getToken();

  // optional: don't add token to auth calls
  if (!token || req.url.includes('/api/auth/')) return next(req);
  // add the Authorization header with the token for all other requests
  return next(req.clone({
    setHeaders: { Authorization: `Bearer ${token}` }
  }));
};
