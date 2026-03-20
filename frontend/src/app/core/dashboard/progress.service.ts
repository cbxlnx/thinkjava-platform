import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { API_BASE_URL } from '../config';

export type MasteryDto = {
  checkpoint: string;
  masteryValue: number;
};

@Injectable({
  providedIn: 'root'
})
export class ProgressService {
  constructor(private http: HttpClient) {}

  getMasterySnapshot(): Observable<MasteryDto[]> {
    return this.http.get<MasteryDto[]>(`${API_BASE_URL}/mastery`);
  }
}