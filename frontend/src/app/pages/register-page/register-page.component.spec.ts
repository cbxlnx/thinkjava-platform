import { ComponentFixture, TestBed } from '@angular/core/testing';
import { of, throwError } from 'rxjs';
import { provideRouter, Router } from '@angular/router';
import { RegisterPageComponent } from './register-page.component';
import { AuthService } from '../../core/auth/auth.service';

describe('RegisterPageComponent', () => {
  let component: RegisterPageComponent;
  let fixture: ComponentFixture<RegisterPageComponent>;
  let authSpy: jasmine.SpyObj<AuthService>;
  let router: Router;

  beforeEach(async () => {
    authSpy = jasmine.createSpyObj<AuthService>('AuthService', ['register']);

    await TestBed.configureTestingModule({
      imports: [RegisterPageComponent],
      providers: [
        provideRouter([]),
        { provide: AuthService, useValue: authSpy },
      ],
    }).compileComponents();

    router = TestBed.inject(Router);
    spyOn(router, 'navigate');
    fixture = TestBed.createComponent(RegisterPageComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('does not submit when the form is invalid', () => {
    component.form.setValue({
      email: 'student@example.com',
      password: '123',
      confirmPassword: '',
    });

    component.submit();

    expect(authSpy.register).not.toHaveBeenCalled();
    expect(router.navigate).not.toHaveBeenCalled();
  });

  it('shows an error when passwords do not match', () => {
    component.form.setValue({
      email: 'student@example.com',
      password: 'password123',
      confirmPassword: 'different-password',
    });

    component.submit();

    expect(component.error).toBe('Passwords do not match');
    expect(authSpy.register).not.toHaveBeenCalled();
  });

  it('registers and navigates to the welcome page on success', () => {
    authSpy.register.and.returnValue(of({ token: 'jwt-token' } as any));
    component.form.setValue({
      email: 'student@example.com',
      password: 'password123',
      confirmPassword: 'password123',
    });

    component.submit();

    expect(authSpy.register).toHaveBeenCalledWith('student@example.com', 'password123');
    expect(router.navigate).toHaveBeenCalledWith(['/welcome']);
    expect(component.error).toBeNull();
  });

  it('shows a fallback error when registration fails without a backend message', () => {
    authSpy.register.and.returnValue(throwError(() => ({})));
    component.form.setValue({
      email: 'student@example.com',
      password: 'password123',
      confirmPassword: 'password123',
    });

    component.submit();

    expect(component.error).toBe('Registration failed');
    expect(router.navigate).not.toHaveBeenCalled();
  });
});
