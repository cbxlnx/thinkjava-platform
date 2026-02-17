import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { environment } from '../../../environments/environment';

export type UserMeResponse = { email: string; firstName: string | null };

@Injectable({ providedIn: 'root' })
export class UsersService {
  private base = environment.apiBaseUrl;

  constructor(private http: HttpClient) {}

  me() {
    return this.http.get<UserMeResponse>(`${this.base}/api/users/me`);
  }

  updateName(firstName: string) {
    return this.http.patch<UserMeResponse>(`${this.base}/api/users/me/name`, { firstName });
  }
}