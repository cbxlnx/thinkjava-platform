import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormControl, FormGroup, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { AuthService } from '../../core/auth/auth.service';

@Component({
  selector: 'app-register-page',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterLink],
  templateUrl: './register-page.component.html',
  styleUrls: ['./register-page.component.css'],

})
export class RegisterPageComponent {
  error: string | null = null;

  form = new FormGroup({
    email: new FormControl('', [Validators.required, Validators.email]),
    password: new FormControl('', [Validators.required, Validators.minLength(6)]),
    confirmPassword: new FormControl('', [Validators.required]),
  });

  constructor(private auth: AuthService, private router: Router) {}

  submit() {
    this.error = null;
    if (this.form.invalid) return;

    const { email, password, confirmPassword } = this.form.getRawValue();
    if (password !== confirmPassword) {
      this.error = 'Passwords do not match';
      return;
}

    this.auth.register(email!, password!).subscribe({
      next: () => this.router.navigate(['/dashboard']),
      error: (e) => this.error = e?.error ?? 'Registration failed',
    });
  }
}
