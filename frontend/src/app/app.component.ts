import { Component, OnInit } from '@angular/core';
import { HttpClientModule, HttpClient } from '@angular/common/http';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [CommonModule, HttpClientModule],
  template: `
    <div style="text-align:center; margin-top:3rem; font-family:system-ui">
      <h1>{{ message }}</h1>
      <p style="opacity:.7">From: http://localhost:8080/api/ping</p>
    </div>
  `
})
export class AppComponent implements OnInit {
  message = 'Loading...';
  constructor(private http: HttpClient) {}
  ngOnInit() {
    this.http.get('http://localhost:8080/api/ping', { responseType: 'text' })
      .subscribe({
        next: res => this.message = res,
        error: () => this.message = '❌ Backend not reachable'
      });
  }
}
