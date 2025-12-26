import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { tap } from 'rxjs';
import { API_BASE_URL } from '../config';

type JwtResponse = { token: string };

@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly tokenKey = 'thinkjava_token';

  constructor(private http: HttpClient) {}

  register(email: string, password: string) {
    return this.http.post<JwtResponse>(`${API_BASE_URL}/auth/register`, { email, password })
      .pipe(tap(res => this.setToken(res.token)));
  }

  login(email: string, password: string) {
    return this.http.post<JwtResponse>(`${API_BASE_URL}/auth/login`, { email, password })
      .pipe(tap(res => this.setToken(res.token)));
  }

  logout() {
    localStorage.removeItem(this.tokenKey);
  }

  getToken(): string | null {
    return localStorage.getItem(this.tokenKey);
  }

  isLoggedIn(): boolean {
    return !!this.getToken();
  }

  private setToken(token: string) {
    localStorage.setItem(this.tokenKey, token);
  }
}
