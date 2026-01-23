import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { API_BASE_URL } from '../config';

export type DiagnosticStatusResponse = { required: boolean; status: string };

@Injectable({ providedIn: 'root' })
export class DiagnosticService {
  constructor(private http: HttpClient) {}

  getStatus() {
    return this.http.get<DiagnosticStatusResponse>(`${API_BASE_URL}/diagnostic/status`);
  }

  getResult() {
    return this.http.get(`${API_BASE_URL}/diagnostic/result`);
  }

  complete(payload: any) {
    return this.http.post(`${API_BASE_URL}/diagnostic/complete`, payload);
  }
}