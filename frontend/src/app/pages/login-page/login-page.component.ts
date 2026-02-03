import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormControl, FormGroup, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { AuthService } from '../../core/auth/auth.service';
import { DiagnosticService } from '../../core/diagnostic/diagnostic.service';

@Component({
  selector: 'app-login-page',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterLink],
  templateUrl: './login-page.component.html',
  styleUrls: ['./login-page.component.css'],
})
export class LoginPageComponent {
  error: string | null = null;

  form = new FormGroup({
    email: new FormControl('', [Validators.required, Validators.email]),
    password: new FormControl('', [Validators.required, Validators.minLength(6)])
  });

  constructor(
    private auth: AuthService,
    private router: Router
  ) {}

  submit() {
  this.error = null;
  if (this.form.invalid) return;

  const { email, password } = this.form.getRawValue();

  this.auth.login(email!, password!).subscribe({
    next: () => this.router.navigate(['/dashboard']), // guard decides if diagnostic is needed
    error: (e) => (this.error = e?.error ?? 'Login failed'),
  });
}
}