import { CommonModule } from '@angular/common';
import { Component, Input } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { LearnApiService } from '../learn-api.service';
import { TutorSearchResult } from '../learn.service';
import { DomSanitizer, SafeHtml } from '@angular/platform-browser';
import { marked } from 'marked';

@Component({
  selector: 'app-ai-tutor',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './ai-tutor.component.html',
  styleUrls: ['./ai-tutor.component.css']
})
export class AiTutorComponent {
  @Input() lessonId = '';
  @Input() topicLabel = '';

  tutorQuestion = '';
  tutorLoading = false;
  tutorError = '';
  tutorMatches: TutorSearchResult[] = [];
  tutorAnswer: SafeHtml = '';

  constructor(
    private learnApi: LearnApiService,
    private sanitizer: DomSanitizer
  ) {}

  askTutor() {
    const question = this.tutorQuestion.trim();
    if (!question || !this.lessonId) return;

    this.tutorLoading = true;
    this.tutorError = '';
    this.tutorMatches = [];
    this.tutorAnswer = '';

    this.learnApi.askTutor({
      lessonId: this.lessonId,
      question
    }).subscribe({
      next: async (res) => {
        this.tutorMatches = res.matches ?? [];

        const rawAnswer = res.answer ?? '';

        const html = await marked.parse(rawAnswer, {
          breaks: true,
          gfm: true
        });

        this.tutorAnswer = this.sanitizer.bypassSecurityTrustHtml(html);
        this.tutorLoading = false;
      },
      error: () => {
        this.tutorError = 'Tutor search failed';
        this.tutorLoading = false;
      }
    });
  }

  askPreset(question: string) {
    this.tutorQuestion = question;
    this.askTutor();
  }
}