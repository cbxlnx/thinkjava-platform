import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink, Router } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { LearnService, LessonSummary, UserLevel } from '../../core/learn/learn.service';

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

  constructor(private learn: LearnService, private router: Router) {}

  ngOnInit(): void {
    this.load();
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

  // ----- counts -----
  get completedCount(): number {
    return this.lessons.filter(l => l.status === 'completed').length;
  }
  get activeCount(): number {
    return this.lessons.filter(l => l.status === 'in_progress').length;
  }
  get upcomingCount(): number {
    return this.lessons.filter(l => l.status === 'not_started').length;
  }

  // ----- groups -----
  get completedLessons(): LessonSummary[] {
    return this.filtered(this.lessons.filter(l => l.status === 'completed'));
  }

  get activeLessons(): LessonSummary[] {
    return this.filtered(this.lessons.filter(l => l.status === 'in_progress'));
  }

  get upcomingLessons(): LessonSummary[] {
    return this.filtered(this.lessons.filter(l => l.status === 'not_started'));
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
}