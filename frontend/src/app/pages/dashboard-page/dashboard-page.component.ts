import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { HttpClient } from '@angular/common/http';
import { Router } from '@angular/router';
import { AuthService } from '../../core/auth/auth.service';
import { API_BASE_URL } from '../../core/config';

@Component({
  selector: 'app-dashboard-page',
  standalone: true,
  imports: [CommonModule],
  template: `
    <h2>Dashboard</h2>

    <button (click)="loadMe()">Call /secure/me</button>
    <button (click)="logout()">Logout</button>

    <pre style="margin-top:12px">{{ result }}</pre>
  `,
})
export class DashboardPageComponent {
  result = '';

  constructor(private http: HttpClient, private auth: AuthService, private router: Router) {}

  loadMe() {
    this.http.get(`${API_BASE_URL}/secure/me`, { responseType: 'text' }).subscribe({
      next: (txt) => (this.result = txt),
      error: (e) => (this.result = `Error: ${e.status} ${e.statusText}`),
    });
  }

  logout() {
    this.auth.logout();
    this.router.navigate(['/login']);
  }
}
