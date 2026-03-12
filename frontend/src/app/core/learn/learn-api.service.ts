import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { API_BASE_URL } from '../config';
import {
  LearnPathResponse,
  LessonResponse,
  LessonQuizSubmitRequest,
  LessonQuizSubmitResponse,
} from './learn.models';
import { LearnRecommendationsResponse } from './learn.service';

@Injectable({ providedIn: 'root' })
export class LearnApiService {
  constructor(private http: HttpClient) {}

  getPath(): Observable<LearnPathResponse> {
    return this.http.get<LearnPathResponse>(`${API_BASE_URL}/learn/path`);
  }

  getLesson(lessonId: string): Observable<LessonResponse> {
    return this.http.get<LessonResponse>(`${API_BASE_URL}/learn/lesson/${lessonId}`);
  }

  getRecommendations(): Observable<LearnRecommendationsResponse> {
  return this.http.get<LearnRecommendationsResponse>(
    `${API_BASE_URL}/learn/recommendations`
  );
}

  submitQuiz(lessonId: string, body: LessonQuizSubmitRequest): Observable<LessonQuizSubmitResponse> {
    return this.http.post<LessonQuizSubmitResponse>(
      `${API_BASE_URL}/learn/lesson/${lessonId}/quiz/submit`,
      body
    );
  }
}