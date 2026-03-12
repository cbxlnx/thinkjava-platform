import {
  ApplicationConfig,
  provideBrowserGlobalErrorListeners,
  provideZoneChangeDetection,
} from '@angular/core';

import { provideRouter } from '@angular/router';
import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { provideMarkdown } from 'ngx-markdown';
import { routes } from './app.routes';
import { authInterceptor } from './core/auth/auth.interceptor';
import { AppComponent } from './app.component';


export const appConfig: ApplicationConfig = {
  providers: [
    provideBrowserGlobalErrorListeners(),
    provideMarkdown(),
    provideZoneChangeDetection({ eventCoalescing: true }),

    // routing
    provideRouter(routes),

    // HTTP + JWT interceptor
    provideHttpClient(withInterceptors([authInterceptor])),
  ],
};
