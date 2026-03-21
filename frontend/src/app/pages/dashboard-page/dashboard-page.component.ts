import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, RouterLink } from '@angular/router';
import { HttpClient } from '@angular/common/http';
import { FormsModule } from '@angular/forms';

import { API_BASE_URL } from '../../core/config';
import { AuthService } from '../../core/auth/auth.service';
import { DiagnosticService } from '../../core/diagnostic/diagnostic.service';
import { UsersService } from '../../core/users/users.service';
import { LearnService, LearnRecommendationsResponse, LessonSummary } from '../../core/learn/learn.service';
import { DashboardService, DashboardSummaryResponse } from '../../core/dashboard/dashboard.service';


type Checkpoint = 'fundamentals' | 'loops' | 'arrays' | 'methods' | 'oop';
type Level = 'Strong' | 'Medium' | 'Weak' | 'Unknown';

type DiagnosticResult = {
  fundamentals: Level;
  loops: Level;
  arrays: Level;
  methods: Level;
  oop: Level;
  startModule?: Checkpoint;
};

@Component({
  selector: 'app-dashboard-page',
  standalone: true,
  imports: [CommonModule, RouterLink, FormsModule],
  templateUrl: './dashboard-page.component.html',
  styleUrls: ['./dashboard-page.component.css'],
})
export class DashboardPageComponent implements OnInit {
  // --- user ---
  displayName = '';
  initials = 'U';

  // --- diagnostic ---
  diag: DiagnosticResult | null = null;

  // --- UI mock data ---
  dayStreak = 14;

  weeklyHours = 19.9;
  weeklyLessons = 12;

  recommendations: LearnRecommendationsResponse | null = null;
  recommendedLesson: LessonSummary | null = null;
  loadingRecommendation = false;
  recommendationError = '';

  dashboardSummary: DashboardSummaryResponse | null = null;

  overallMasteryPercent = 0;

  completedLessons = 0;
  totalLessons = 0;
  courseProgressPercent = 0;

  // 7 values Mon..Sun (0..10-ish)
  weeklyBars = [1, 2, 1, 3, 2, 4, 3];

  checkpointProgressItems: {
    label: string;
    completed: number;
    total: number;
    percent: number;
  }[] = [];

  recentActivity = [
    { title: 'Completed: Inheritance & Polymorphism', time: '2 hours ago', pill: '82%' },
    { title: 'Earned Badge: OOP Master', time: '1 hour ago', pill: '' },
    { title: 'Started: Advanced Collections', time: 'Yesterday', pill: '' },
    { title: 'Quiz Passed: Abstract Classes', time: '2 days ago', pill: '88%' },
  ];



  // current focus mock
  currentFocusTitle = 'Object-Oriented Programming';
  currentFocusSubtitle = 'Classes, Objects & Inheritance';
  currentFocusCode = `public class Student {
  private String name;
  private int age;

  public int getAge() {
    return age;
  }

  public Student(String name) {
    this.name = name;
  }
}`;
  currentFocusProgressLabel = '3/5';
  currentFocusProgressPercent = 60;

  constructor(
    private users: UsersService,
    private auth: AuthService,
    private diagnosticService: DiagnosticService,
    private learnService: LearnService,
    private router: Router,
    private dashboardService: DashboardService,
    private learn: LearnService
  ) { }

  ngOnInit(): void {
    this.loadMe();
    this.loadDiagnostic();
    this.loadRecommendations();
    this.loadDashboardSummary();
    this.learn.getAllLessons().subscribe({
      next: (res) => {
        this.calculateCourseProgress(res.lessons);
        this.buildCheckpointProgress(res.lessons);
      },
      error: (err) => {
        console.error('Failed to load lessons for dashboard progress', err);
      }
    });
  }

  // -----------------------------
  // DATA LOADING
  // -----------------------------
  loadRecommendations(): void {
    this.loadingRecommendation = true;
    this.recommendationError = '';

    this.learnService.getRecommendations().subscribe({
      next: (res) => {
        this.recommendations = res;
        this.recommendedLesson = res.recommendedLessons?.[0] ?? null;
        this.loadingRecommendation = false;
      },
      error: (err) => {
        console.error('Failed to load recommendations', err);
        this.recommendationError = 'Could not load recommendations.';
        this.loadingRecommendation = false;
      }
    });
  }
 // Later: separate out into a "recommendations" service that can be 
 // used across dashboard and learn pages, and can return both pre-diagnostic and post-quiz recommendations based on context
  loadDashboardSummary() {
    this.dashboardService.getSummary().subscribe({
      next: (res) => {
        this.dashboardSummary = res;

        this.currentFocusTitle = res.currentFocus?.title ?? 'No active lesson yet';
        this.currentFocusSubtitle = res.currentFocus
          ? this.getCheckpointLabel(res.currentFocus.checkpoint)
          : 'Start learning to build momentum';

        this.currentFocusProgressPercent = res.currentFocus?.progressPercent ?? 0;
        this.currentFocusProgressLabel = res.currentFocus
          ? `${Math.round((res.currentFocus.progressPercent / 100) * 5)}/5`
          : '0/5';

        this.weeklyLessons = res.weeklyLessons;
        this.weeklyHours = +(res.weeklyMinutes / 60).toFixed(1);

        this.recentActivity = res.recentActivity ?? [];

        this.currentFocusCode = this.getSnippetForCheckpoint(
          res.currentFocus?.checkpoint
        );


      },
      error: (err) => {
        console.error('Failed to load dashboard summary', err);
        this.dashboardSummary = null;

        this.currentFocusTitle = 'No active lesson yet';
        this.currentFocusSubtitle = 'Start learning to build momentum';
        this.currentFocusProgressPercent = 0;
        this.currentFocusProgressLabel = '0/5';
        this.weeklyLessons = 0;
        this.weeklyHours = 0;
        this.recentActivity = [];
      },
    });
  }

  getCheckpointLabel(checkpoint?: string | null): string {
    if (!checkpoint) return '';
    switch (checkpoint) {
      case 'fundamentals': return 'Fundamentals';
      case 'loops': return 'Loops';
      case 'arrays': return 'Arrays';
      case 'methods': return 'Methods';
      case 'oop': return 'Object-Oriented Programming';
      default: return checkpoint;
    }
  }

  // -----------------------------
  // DATA LOADING
  // -----------------------------
  loadMe() {
    this.users.me().subscribe({
      next: (me) => {
        const first = (me.firstName ?? '').trim();
        if (first) this.applyName(first);
        else {
          this.displayName = '';
          this.initials = 'U';
        }
      },
      error: () => {
        this.displayName = '';
        this.initials = 'U';
      },
    });
  }

  loadDiagnostic() {
    this.diagnosticService.getResult().subscribe({
      next: (res) => (this.diag = res as DiagnosticResult),
      error: () => (this.diag = null),
    });
  }

  // -----------------------------
  // NAME GATE (first time)
  // -----------------------------

  private applyName(firstName: string) {
    this.displayName = firstName;
    this.initials = (firstName[0] || 'U').toUpperCase();
  }
  // -----------------------------
  // ACTIONS
  // -----------------------------  
  get aiStartModuleLabel(): string {
    const start = this.diag?.startModule || 'fundamentals';
    const map: Record<string, string> = {
      fundamentals: 'Fundamentals',
      loops: 'Loops',
      arrays: 'Arrays',
      methods: 'Methods',
      oop: 'OOP',
    };
    return map[start] ?? 'Fundamentals';
  }

  // SVG ring math 
  get ringDasharray(): number {
    return 289;
  }

  get ringDashoffset(): number {
    const p = this.masteryPercent;
    return this.ringDasharray - (this.ringDasharray * p) / 100;
  }

  get masteryPercent(): number {
    return this.dashboardSummary?.masteryPercent ?? 0;
  }

  get masteryLabel(): 'Beginner' | 'Intermediate' | 'Advanced' {
    return this.dashboardSummary?.masteryLabel ?? 'Beginner';
  }

  get topicsCompletedText(): string {
    return this.dashboardSummary?.topicsCompletedText ?? '0/12';
  }

  get quizScoreAvgText(): string {
    return this.dashboardSummary?.quizScoreAvgText ?? '0%';
  }

  get weakAreasLabel(): string {
    return (this.recommendations?.weakAreas ?? [])
      .map(area => this.getCheckpointLabel(area))
      .join(', ');
  }

  // -----------------------------
  // ACTIONS
  // -----------------------------
  takeQuiz() {
    this.router.navigate(['/diagnostic']);
  }

  continueLesson() {
    // later: route to current lesson
    this.router.navigate(['/learn']);
  }

  askTutor() {
    // later: open tutor chat
    this.router.navigate(['/tutor']);
  }

  startAiRecommendation() {
    // later: go to recommended module
    this.router.navigate(['/learn'], { queryParams: { start: this.diag?.startModule || 'fundamentals' } });
  }

  logout() {
    this.auth.logout();
    this.router.navigate(['/login']);
  }
  // For the code snippet on the dashboard current focus card - could be based on checkpoint or specific lesson
  getSnippetForCheckpoint(checkpoint?: string | null): string {
    switch (checkpoint) {
      case 'fundamentals':
        return `int age = 21;
String name = "Alice";
boolean isActive = true;`;

      case 'loops':
        return `for (int i = 0; i < 5; i++) {
    System.out.println(i);
}`;

      case 'arrays':
        return `int[] scores = {85, 90, 78};
System.out.println(scores[0]);`;

      case 'methods':
        return `public int add(int a, int b) {
    return a + b;
}`;

      case 'oop':
        return `class Car {
    String brand;

    Car(String brand) {
        this.brand = brand;
    }
}`;

      default:
        return `// Start learning to see code here`;
    }
  }
  // For the checkpoint mastery bars on the dashboard
  get checkpointMasteryItems(): { key: string; label: string; value: number }[] {
  const cp = this.dashboardSummary?.checkpointMastery ?? {};

  return [
    { key: 'fundamentals', label: 'Fundamentals', value: this.clampPercent(cp['fundamentals']) },
    { key: 'loops', label: 'Loops', value: this.clampPercent(cp['loops']) },
    { key: 'arrays', label: 'Arrays', value: this.clampPercent(cp['arrays']) },
    { key: 'methods', label: 'Methods', value: this.clampPercent(cp['methods']) },
    { key: 'oop', label: 'OOP', value: this.clampPercent(cp['oop']) },
  ];
}

get chartLinePoints(): string {
  const items = this.checkpointMasteryItems;
  const height = 320;
  const width = 500;

  if (!items || items.length === 0) {
    return '';
  }

  const step = width / (items.length - 1);

  return items
    .map((item, index) => {
      const x = index * step;
      const y = height - (item.value / 100) * height;
      return `${x},${y}`;
    })
    .join(' ');
}
private clampPercent(value: number | null | undefined): number {
  return Math.max(0, Math.min(100, Math.round(value ?? 0)));
}
  // For the overall course progress circle on the dashboard - could be based on
  //  number of lessons completed vs total, or could be an average of checkpoint mastery percentages
  private calculateCourseProgress(lessons: any[]): void {
    this.totalLessons = lessons.length;
    this.completedLessons = lessons.filter(l => l.status === 'completed').length;

    this.courseProgressPercent =
      this.totalLessons > 0
        ? Math.round((this.completedLessons / this.totalLessons) * 100)
        : 0;
  }
  // For the checkpoint progress bars on the dashboard - calculates
  //  completed vs total lessons for each checkpoint to show progress within that topic
  private buildCheckpointProgress(lessons: any[]): void {
  const checkpointOrder = ['fundamentals', 'loops', 'arrays', 'methods', 'oop'];

  const labelMap: Record<string, string> = {
    fundamentals: 'Fundamentals',
    loops: 'Loops',
    arrays: 'Arrays',
    methods: 'Methods',
    oop: 'OOP'
  };

  this.checkpointProgressItems = checkpointOrder.map((checkpoint) => {
    const topicLessons = lessons.filter((l) => l.checkpoint === checkpoint);
    const completed = topicLessons.filter((l) => l.status === 'completed').length;
    const total = topicLessons.length;
    const percent = total > 0 ? Math.round((completed / total) * 100) : 0;

    return {
      label: labelMap[checkpoint] ?? checkpoint,
      completed,
      total,
      percent
    };
  });
}

}