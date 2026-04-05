import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { AuthService } from './auth.service';

describe('AuthService', () => {
  // the service under test
  let service: AuthService;

  // lets the test inspect and flush outgoing http requests
  let httpMock: HttpTestingController;

  beforeEach(() => {
    // clears any saved token before each test starts
    localStorage.clear();

    // sets up angular http testing utilities
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });

    // resolves the auth service and the http testing controller
    service = TestBed.inject(AuthService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    // verifies no unexpected http requests are left open
    httpMock.verify();

    // clears local storage so tests stay isolated from each other
    localStorage.clear();
  });

  it('stores the token after login', () => {
    // triggers the login request
    service.login('student@example.com', 'password123').subscribe();

    // verifies the request shape sent to the backend
    const req = httpMock.expectOne('http://localhost:8080/api/auth/login');
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual({
      email: 'student@example.com',
      password: 'password123',
    });

    req.flush({ token: 'login-token' });

    // verifies the token is stored and the user is considered logged in
    expect(service.getToken()).toBe('login-token');
    expect(service.isLoggedIn()).toBeTrue();
  });

  it('stores the token after registration', () => {
    // triggers the register request
    service.register('student@example.com', 'password123').subscribe();

    // verifies registration goes to the expected endpoint
    const req = httpMock.expectOne('http://localhost:8080/api/auth/register');
    expect(req.request.method).toBe('POST');
    req.flush({ token: 'register-token' });

    // verifies the returned token is persisted
    expect(service.getToken()).toBe('register-token');
  });

  it('removes the token on logout', () => {
    // seeds local storage with an existing token
    localStorage.setItem('thinkjava_token', 'saved-token');

    // logs the user out
    service.logout();

    // verifies the token is removed and login state becomes false
    expect(service.getToken()).toBeNull();
    expect(service.isLoggedIn()).toBeFalse();
  });
});
