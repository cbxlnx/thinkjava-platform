import { Component, inject } from '@angular/core';
import { FormBuilder, Validators, ReactiveFormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { CommonModule } from '@angular/common';
import { UsersService } from '../../core/users/users.service';
import { DiagnosticService } from '../../core/diagnostic/diagnostic.service';

@Component({
  selector: 'app-welcome-page',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './welcome-page.component.html',
  styleUrls: ['./welcome-page.component.css']
})
export class WelcomePageComponent {
  private fb = inject(FormBuilder);
  private users = inject(UsersService);
  private diagnostic = inject(DiagnosticService);
  private router = inject(Router);

  loading = false;
  error = '';

  form = this.fb.nonNullable.group({
    firstName: ['', [Validators.required, Validators.maxLength(30)]],
  });

  save() {
    if (this.form.invalid || this.loading) return;

    this.loading = true;
    this.error = '';

    const name = this.form.controls.firstName.value.trim();

    if (!name) {
      this.error = 'Please enter your name.';
      this.loading = false;
      return;
    }

    this.users.updateName(name).subscribe({
      next: () => {
        // after name is saved, check diagnostic requirement
        this.diagnostic.getStatus().subscribe({
          next: (s) => {
            this.router.navigateByUrl(
              s.required ? '/diagnostic' : '/dashboard'
            );
          },
          error: () => {
            // fallback to diagnostic if status fails
            this.router.navigateByUrl('/diagnostic');
          }
        });
      },
      error: (e) => {
        this.error = e?.error?.message ?? 'Could not save name';
        this.loading = false;
      }
    });
  }
}