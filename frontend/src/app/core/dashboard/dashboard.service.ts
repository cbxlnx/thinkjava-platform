import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { API_BASE_URL } from '../config';
import { LessonSummary } from '../learn/learn.service';

export type ActivityItem = {
  title: string;
  time: string;
  pill: string;
};

export type DashboardSummaryResponse = {
  masteryPercent: number;
  masteryLabel: 'Beginner' | 'Intermediate' | 'Advanced';
  topicsCompletedText: string;
  quizScoreAvgText: string;
  weeklyMinutes: number;
  weeklyLessons: number;
  currentFocus: LessonSummary | null;
  recentActivity: ActivityItem[];
  checkpointMastery: Record<string, number>;
};

@Injectable({ providedIn: 'root' })
export class DashboardService {
  constructor(private http: HttpClient) {}

  getSummary(): Observable<DashboardSummaryResponse> {
    return this.http.get<DashboardSummaryResponse>(`${API_BASE_URL}/dashboard/summary`);
  }
}