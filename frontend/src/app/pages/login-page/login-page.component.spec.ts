import { ComponentFixture, TestBed } from '@angular/core/testing';
import { of, throwError } from 'rxjs';
import { provideRouter, Router } from '@angular/router';
import { LoginPageComponent } from './login-page.component';
import { AuthService } from '../../core/auth/auth.service';

describe('LoginPageComponent', () => {
  let component: LoginPageComponent;
  let fixture: ComponentFixture<LoginPageComponent>;
  let authSpy: jasmine.SpyObj<AuthService>;
  let router: Router;

  beforeEach(async () => {
    authSpy = jasmine.createSpyObj<AuthService>('AuthService', ['login']);

    await TestBed.configureTestingModule({
      imports: [LoginPageComponent],
      providers: [
        provideRouter([]),
        { provide: AuthService, useValue: authSpy },
      ],
    }).compileComponents();

    router = TestBed.inject(Router);
    spyOn(router, 'navigate');
    fixture = TestBed.createComponent(LoginPageComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('does not submit when the form is invalid', () => {
    component.form.setValue({
      email: 'invalid-email',
      password: '123',
    });

    component.submit();

    expect(authSpy.login).not.toHaveBeenCalled();
    expect(router.navigate).not.toHaveBeenCalled();
  });

  it('logs in and navigates to the dashboard on success', () => {
    authSpy.login.and.returnValue(of({ token: 'jwt-token' } as any));
    component.form.setValue({
      email: 'student@example.com',
      password: 'password123',
    });

    component.submit();

    expect(authSpy.login).toHaveBeenCalledWith('student@example.com', 'password123');
    expect(router.navigate).toHaveBeenCalledWith(['/dashboard']);
    expect(component.error).toBeNull();
  });

  it('shows a fallback error message when login fails without a backend message', () => {
    authSpy.login.and.returnValue(throwError(() => ({})));
    component.form.setValue({
      email: 'student@example.com',
      password: 'password123',
    });

    component.submit();

    expect(component.error).toBe('Login failed');
    expect(router.navigate).not.toHaveBeenCalled();
  });
});
