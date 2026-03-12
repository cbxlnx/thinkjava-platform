import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { API_BASE_URL } from '../config';

export type Checkpoint = 'fundamentals' | 'loops' | 'arrays' | 'methods' | 'oop';
export type LessonStatus = 'completed' | 'in_progress' | 'not_started';
export type UserLevel = 'Beginner' | 'Intermediate' | 'Advanced';

export type LearnPathResponse = {
  recommendedLessonId: string;
  startCheckpoint: Checkpoint;
  mastery: Record<Checkpoint, number>;
};

export type LessonResponse = {
  lesson: {
    id: string;
    checkpoint: Checkpoint;
    title: string;
    orderIndex: number;
    estimatedMinutes?: number;
  };
  sections: Array<{
    order: number;
    markdown: string;
  }>;
  quiz: {
    questions: Array<{
      id: string;
      prompt: string;
      options: string[];
    }>;
  };
};

export type LessonQuizSubmitRequest = {
  answers: Record<string, string>;
};

export type LessonQuizSubmitResponse = {
  score: number;
  passed: boolean;
  updatedCheckpointMastery: number;
  recommendedNextLessonId: string;
};

export type LessonSummary = {
  id: string;
  title: string;
  checkpoint: Checkpoint;
  orderIndex: number;
  estimatedMinutes?: number;
  difficulty: number; // 1..3
  levelTag: UserLevel;
  status: LessonStatus;
  progressPercent: number; // 0..100
  locked: boolean;
};

export type LearnRecommendationsResponse = {
  primaryLessonId: string | null;
  primaryCheckpoint: Checkpoint | null;
  reason: string;
  weakAreas: Checkpoint[];
  recommendedLessons: LessonSummary[];
};

export type AllLessonsResponse = {
  userLevel: UserLevel;
  lessons: LessonSummary[];
};

@Injectable({ providedIn: 'root' })
export class LearnService {
  constructor(private http: HttpClient) { }

  getPath(): Observable<LearnPathResponse> {
    return this.http.get<LearnPathResponse>(`${API_BASE_URL}/learn/path`);
  }

  getAllLessons(): Observable<AllLessonsResponse> {
    return this.http.get<AllLessonsResponse>(`${API_BASE_URL}/learn/lessons`);
  }

  getLesson(lessonId: string): Observable<LessonResponse> {
    return this.http.get<LessonResponse>(`${API_BASE_URL}/learn/lesson/${lessonId}`);
  }

  submitQuiz(lessonId: string, body: LessonQuizSubmitRequest): Observable<LessonQuizSubmitResponse> {
    return this.http.post<LessonQuizSubmitResponse>(`${API_BASE_URL}/learn/lesson/${lessonId}/quiz/submit`, body);
  }

  getRecommendations(): Observable<LearnRecommendationsResponse> {
    return this.http.get<LearnRecommendationsResponse>(
      `${API_BASE_URL}/learn/recommendations`
    );
  }

  getCurrentFocus(): Observable<LessonSummary> {
    return this.http.get<LessonSummary>(`${API_BASE_URL}/learn/current-focus`);
  }
}