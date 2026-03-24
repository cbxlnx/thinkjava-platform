import { Component, OnInit } from '@angular/core';
import { HttpClientModule, HttpClient } from '@angular/common/http';
import { CommonModule } from '@angular/common';
import { API_BASE_URL } from './core/config';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [CommonModule, HttpClientModule],
  template: `
    <div style="text-align:center; margin-top:3rem; font-family:system-ui">
      <h1>{{ message }}</h1>
      <p style="opacity:.7">From: {{ API_BASE_URL }}/api/ping</p>
    </div>
  `
})
export class AppComponent implements OnInit {
  message = 'Loading...';
  API_BASE_URL = API_BASE_URL;
  constructor(private http: HttpClient) {}
  ngOnInit() {
    this.http.get(`${API_BASE_URL}/api/ping`, { responseType: 'text' })
      .subscribe({
        next: res => this.message = res,
        error: () => this.message = 'Backend not reachable'
      });
  }
}
