import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink, Router } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { LearnService, LearnRecommendationsResponse, LessonSummary, UserLevel } from '../../core/learn/learn.service';

type FilterChip = 'All' | 'Beginner' | 'Intermediate' | 'Advanced' | 'Completed';

@Component({
  selector: 'app-learn-index-page',
  standalone: true,
  imports: [CommonModule, RouterLink, FormsModule],
  templateUrl: './learn-index-page.component.html',
  styleUrls: ['./learn-index-page.component.css'],
})
export class LearnIndexPageComponent implements OnInit {
  loading = true;
  errorMsg = '';

  userLevel: UserLevel = 'Beginner';
  lessons: LessonSummary[] = [];

  // UI
  search = '';
  chip: FilterChip = 'All';

  recommendations: LearnRecommendationsResponse | null = null;
  topRecommendedLessons: LessonSummary[] = [];
  loadingRecommendations = false;

  constructor(private learn: LearnService, private router: Router) { }

  ngOnInit(): void {
    this.load();
    this.loadRecommendations();
  }

  load() {
    this.loading = true;
    this.errorMsg = '';

    this.learn.getAllLessons().subscribe({
      next: (res) => {
        this.userLevel = res.userLevel;
        this.lessons = [...res.lessons].sort((a, b) => {
          if (a.checkpoint !== b.checkpoint) return a.checkpoint.localeCompare(b.checkpoint);
          return a.orderIndex - b.orderIndex;
        });
        this.loading = false;
      },
      error: (err) => {
        console.error(err);
        this.errorMsg = 'Failed to load lessons.';
        this.loading = false;
      },
    });
  }
  

  loadRecommendations(): void {
    this.loadingRecommendations = true;

    this.learn.getRecommendations().subscribe({
      next: (res) => {
        this.recommendations = res;
        this.topRecommendedLessons = res.recommendedLessons.slice(0, 3);
        this.loadingRecommendations = false;
      },
      error: (err) => {
        console.error('Failed to load learn recommendations', err);
        this.loadingRecommendations = false;
      }
    });
  }

  getCheckpointLabel(checkpoint?: string | null): string {
    if (!checkpoint) return '';
    switch (checkpoint) {
      case 'fundamentals': return 'Fundamentals';
      case 'loops': return 'Loops';
      case 'arrays': return 'Arrays';
      case 'methods': return 'Methods';
      case 'oop': return 'OOP';
      default: return checkpoint;
    }
  }

  getCtaLabel(lesson: LessonSummary): string {
    return lesson.status === 'in_progress' ? 'Continue' : 'Start';
  }

  // ----- counts -----
  get completedCount(): number {
    return this.lessons.filter(l => l.status === 'completed').length;
  }

  get activeCount(): number {
    return this.lessons.filter(l => !l.locked && l.status !== 'completed').length;
  }

  get upcomingCount(): number {
    return this.lessons.filter(l => l.locked && l.status !== 'completed').length;
  }

  // ----- groups -----
  get completedLessons(): LessonSummary[] {
  return this.filtered(
    this.lessons
      .filter(l => l.status === 'completed')
      .sort((a, b) => {
        if (a.checkpoint !== b.checkpoint) return a.checkpoint.localeCompare(b.checkpoint);
        return a.orderIndex - b.orderIndex;
      })
  );
}

get activeLessons(): LessonSummary[] {
  return this.filtered(
    this.lessons
      .filter(l => !l.locked && l.status !== 'completed')
      .sort((a, b) => {
        // in_progress first
        if (a.status === 'in_progress' && b.status !== 'in_progress') return -1;
        if (a.status !== 'in_progress' && b.status === 'in_progress') return 1;

        // then keep normal lesson order
        if (a.checkpoint !== b.checkpoint) return a.checkpoint.localeCompare(b.checkpoint);
        return a.orderIndex - b.orderIndex;
      })
  );
}

get upcomingLessons(): LessonSummary[] {
  return this.filtered(
    this.lessons
      .filter(l => l.locked && l.status !== 'completed')
      .sort((a, b) => {
        if (a.checkpoint !== b.checkpoint) return a.checkpoint.localeCompare(b.checkpoint);
        return a.orderIndex - b.orderIndex;
      })
  );
}

  // ----- filtering -----
  setChip(c: FilterChip) {
    this.chip = c;
  }

  private filtered(list: LessonSummary[]): LessonSummary[] {
    const q = (this.search || '').trim().toLowerCase();

    return list.filter((l) => {
      // search
      const matchesSearch =
        !q ||
        l.title.toLowerCase().includes(q) ||
        l.checkpoint.toLowerCase().includes(q) ||
        l.levelTag.toLowerCase().includes(q);

      if (!matchesSearch) return false;

      // chip filter
      if (this.chip === 'All') return true;
      if (this.chip === 'Completed') return l.status === 'completed';
      return l.levelTag === this.chip;
    });
  }

  // ----- actions -----
  openLesson(lesson: LessonSummary) {
    if (lesson.locked) return;
    this.router.navigate(['/learn/lesson', lesson.id]);
  }

  labelForCheckpoint(cp: string): string {
    const map: Record<string, string> = {
      fundamentals: 'Fundamentals',
      loops: 'Loops',
      arrays: 'Arrays',
      methods: 'Methods',
      oop: 'OOP',
    };
    return map[cp] ?? cp;
  }

  getStatusLabel(status: string): string {
  switch (status) {
    case 'completed':
      return 'Completed';
    case 'in_progress':
      return 'In progress';
    default:
      return 'Not started';
  }
}

  
}