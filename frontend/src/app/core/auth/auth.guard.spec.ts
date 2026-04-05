import { TestBed } from '@angular/core/testing';
import { Router, UrlTree, provideRouter } from '@angular/router';
import { firstValueFrom, of, throwError } from 'rxjs';
import { authGuard } from './auth.guard';
import { AuthService } from './auth.service';
import { DiagnosticService } from '../diagnostic/diagnostic.service';
import { UsersService } from '../users/users.service';

describe('authGuard', () => {
  // mocks the auth service used to determine whether the user is logged in
  let authSpy: jasmine.SpyObj<AuthService>;

  // mocks the diagnostic service used by the guard after onboarding checks
  let diagnosticSpy: jasmine.SpyObj<DiagnosticService>;

  // mocks the user profile service used to enforce name completion
  let usersSpy: jasmine.SpyObj<UsersService>;

  // real test router used to create and serialize url trees
  let router: Router;

  beforeEach(() => {
    // creates spy objects for each dependency used by the guard
    authSpy = jasmine.createSpyObj<AuthService>('AuthService', ['isLoggedIn']);
    diagnosticSpy = jasmine.createSpyObj<DiagnosticService>('DiagnosticService', ['getStatus']);
    usersSpy = jasmine.createSpyObj<UsersService>('UsersService', ['me']);

    // registers the guard dependencies in the angular test injector
    TestBed.configureTestingModule({
      providers: [
        provideRouter([]),
        { provide: AuthService, useValue: authSpy },
        { provide: DiagnosticService, useValue: diagnosticSpy },
        { provide: UsersService, useValue: usersSpy },
      ],
    });

    router = TestBed.inject(Router);
  });

  // runs the guard inside angular injection context and normalizes the return shape
  async function runGuard(url: string): Promise<any> {
    const result = TestBed.runInInjectionContext(() =>
      authGuard({} as any, { url } as any)
    ) as any;

    if (result?.subscribe) {
      return firstValueFrom(result);
    }

    if (result?.then) {
      return result;
    }

    return result;
  }

  it('redirects anonymous users to login', async () => {
    // simulates a logged-out user trying to open a protected route
    authSpy.isLoggedIn.and.returnValue(false);

    const result = await runGuard('/dashboard');

    expect(result instanceof UrlTree).toBeTrue();
    expect(router.serializeUrl(result as UrlTree)).toBe('/login');
  });

  it('allows logged in users to access onboarding pages directly', async () => {
    // onboarding routes should stay accessible once the user is authenticated
    authSpy.isLoggedIn.and.returnValue(true);

    const result = await runGuard('/diagnostic');

    expect(result).toBeTrue();
    expect(usersSpy.me).not.toHaveBeenCalled();
  });

  it('redirects to welcome when the user profile has no first name', async () => {
    // simulates a user who is authenticated but has not completed profile setup
    authSpy.isLoggedIn.and.returnValue(true);
    usersSpy.me.and.returnValue(of({ email: 'student@example.com', firstName: '   ' }));

    const result = await runGuard('/dashboard');

    expect(result instanceof UrlTree).toBeTrue();
    expect(router.serializeUrl(result as UrlTree)).toBe('/welcome');
  });

  it('redirects to diagnostic when onboarding is complete but diagnostic is still required', async () => {
    // simulates a user with a name who still needs to complete the diagnostic
    authSpy.isLoggedIn.and.returnValue(true);
    usersSpy.me.and.returnValue(of({ email: 'student@example.com', firstName: 'Kate' }));
    diagnosticSpy.getStatus.and.returnValue(of({ required: true, status: 'NOT_STARTED' }));

    const result = await runGuard('/dashboard');

    expect(result instanceof UrlTree).toBeTrue();
    expect(router.serializeUrl(result as UrlTree)).toBe('/diagnostic');
  });

  it('allows access when the user has a name and diagnostic is complete', async () => {
    // simulates the fully onboarded happy path
    authSpy.isLoggedIn.and.returnValue(true);
    usersSpy.me.and.returnValue(of({ email: 'student@example.com', firstName: 'Kate' }));
    diagnosticSpy.getStatus.and.returnValue(of({ required: false, status: 'COMPLETED' }));

    const result = await runGuard('/dashboard');

    expect(result).toBeTrue();
  });

  it('redirects to welcome when loading the user profile fails', async () => {
    // simulates a failure while fetching user onboarding data
    authSpy.isLoggedIn.and.returnValue(true);
    usersSpy.me.and.returnValue(throwError(() => new Error('profile failed')));

    const result = await runGuard('/dashboard');

    expect(result instanceof UrlTree).toBeTrue();
    expect(router.serializeUrl(result as UrlTree)).toBe('/welcome');
  });
});
