import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { DomSanitizer, SafeHtml, SafeResourceUrl } from '@angular/platform-browser';
import { LearnApiService } from '../../core/learn/learn-api.service';
import { LessonResponse } from '../../core/learn/learn.models';
import { MarkdownModule } from 'ngx-markdown';
import { HostListener } from '@angular/core';
import { LessonSummary } from '../../core/learn/learn.service';

type UiBlock = LessonResponse['blocks'][number] & {
  payloadObj?: any;
  safeVideoUrl?: SafeResourceUrl | null;
};

@Component({
  selector: 'app-learn-lesson-page',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink, MarkdownModule],
  templateUrl: './learn-lesson-page.component.html',
  styleUrls: ['./learn-lesson-page.component.css'],
})
export class LearnLessonPageComponent implements OnInit {
  loading = true;
  error = '';

  lessonId = '';
  data: LessonResponse | null = null;
  lessonProgress = 0;

  answers: Record<string, string> = {};
  submitting = false;
  submitResult: { score: number; passed: boolean; nextId: string; updated: number } | null = null;
  uiBlocks: UiBlock[] = [];

  lastSavedProgress = 0;
  savingProgress = false;

  postQuizRecommendations: LessonSummary[] = [];
  loadingPostQuizRecommendations = false;

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private learnApi: LearnApiService,
    private sanitizer: DomSanitizer
  ) { }

  ngOnInit(): void {
     this.route.paramMap.subscribe(params => {
    this.lessonId = this.route.snapshot.paramMap.get('id') || '';
    if (!this.lessonId) {
      this.error = 'Missing lesson id';
      this.loading = false;
      return;
    }
    this.loadLesson();
    });
  }

  loadLesson() {
    this.loading = true;
    this.error = '';
    this.submitResult = null;
    this.answers = {};

    this.learnApi.getLesson(this.lessonId).subscribe({
      next: (res) => {
        this.data = res;

        const blocksSorted = [...(res.blocks ?? [])].sort((a, b) => (a.order ?? 0) - (b.order ?? 0));

        this.uiBlocks = blocksSorted.map(b => ({
          ...b,
          payloadObj: this.parsePayload(b.payload),
          safeVideoUrl: b.type === 'VIDEO' ? this.videoSrc(b.videoUrl ?? null) : null
        }));

        this.loading = false;
      },
      error: () => {
        this.error = 'Failed to load lesson';
        this.loading = false;
      },
    });
  }

  trackByBlock = (_: number, b: UiBlock) => `${b.order}-${b.type}`;

  formatMarkdown(md: string): string {
    const esc = (s: string) =>
      s.replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;');

    return esc(md ?? '');
  }

  setAnswer(qId: string, opt: string) {
    this.answers[qId] = opt;
  }

  canSubmit(): boolean {
    const qs = this.data?.quiz?.questions || [];
    if (!qs.length) return false;
    return qs.every((q) => !!this.answers[q.id]);
  }

 submitQuiz() {
  if (!this.data) return;
  if (!this.canSubmit()) return;

  this.submitting = true;
  this.submitResult = null;
  this.postQuizRecommendations = [];
  this.loadingPostQuizRecommendations = false;

  this.learnApi.submitQuiz(this.lessonId, { answers: this.answers }).subscribe({
    next: (res) => {
      this.submitResult = {
        score: res.score,
        passed: res.passed,
        nextId: res.recommendedNextLessonId,
        updated: res.updatedCheckpointMastery,
      };

      this.submitting = false;

      if (res.passed) {
        this.loadPostQuizRecommendations();
      }
    },
    error: () => {
      this.submitting = false;
      this.error = 'Quiz submit failed';
    },
  });
}

loadPostQuizRecommendations(): void {
  this.loadingPostQuizRecommendations = true;

  this.learnApi.getRecommendations().subscribe({
    next: (res) => {
      console.log('Post-quiz recommendations response', res);

      this.postQuizRecommendations = (res.recommendedLessons || [])
        .filter(l => l.id !== this.lessonId)
        .slice(0, 3);

      this.loadingPostQuizRecommendations = false;
    },
    error: (err) => {
      console.error('Failed to load post-quiz recommendations', err);
      this.loadingPostQuizRecommendations = false;
    }
  });
}

  videoSrc(url?: string | null): SafeResourceUrl | null {
    if (!url) return null;

    let u = url.trim();

    // if it's just the ID
    if (/^[a-zA-Z0-9_-]{11}$/.test(u)) {
      u = `https://www.youtube.com/embed/${u}`;
    }

    // youtu.be/<id>
    const short = u.match(/youtu\.be\/([^?&/]+)/);
    if (short?.[1]) u = `https://www.youtube.com/embed/${short[1]}`;

    // youtube.com/watch?v=<id>
    const watch = u.match(/[?&]v=([^&]+)/);
    if (watch?.[1]) u = `https://www.youtube.com/embed/${watch[1]}`;

    return this.sanitizer.bypassSecurityTrustResourceUrl(u);
  }

  normalizeMarkdown(md?: string | null): string {
    if (!md) return '';
    return md
      .replace(/\\n/g, '\n')
      .replace(/\\"/g, '"');
  }

  goNext() {
    const next = this.submitResult?.nextId;
    if (!next) return;
    this.router.navigate(['/learn/lesson', next]);
  }

  sectionLabel(type: string) {
    switch (type) {
      case 'MARKDOWN': return 'CONCEPTS';
      case 'VIDEO': return 'VIDEO';
      case 'INLINE_QUIZ': return 'CHECK';
      default: return 'SECTION';
    }
  }

  // Grab the first markdown heading (# ... or ## ...)
  extractTitle(md?: string | null): string {
    if (!md) return '';
    const clean = this.normalizeMarkdown(md);
    const m = clean.match(/^\s{0,3}#{1,6}\s+(.+)\s*$/m);
    return m?.[1]?.trim() || 'Section';
  }

  // Remove the first heading from markdown so we don't show it twice
  stripTitle(md?: string | null): string {
    if (!md) return '';
    const clean = this.normalizeMarkdown(md);
    // remove only the first heading line
    return clean.replace(/^\s{0,3}#{1,6}\s+.+\s*\n?/, '');
  }


  parsePayload(payload?: any): any | null {
    if (payload == null) return null;

    // If backend already returns JSON (object), just use it
    if (typeof payload === 'object') return payload;

    // If backend returns a JSON string, parse it
    if (typeof payload === 'string') {
      const s = payload.trim();
      if (!s) return null;
      try {
        return JSON.parse(s);
      } catch {
        return null;
      }
    }

    return null;
  }

  highlightJava(src: string): SafeHtml {
    // 1) Escape HTML first
    let s = (src || '')
      .replace(/&/g, '&amp;')
      .replace(/</g, '&lt;')
      .replace(/>/g, '&gt;');

    // 2) Wrap comments + strings first (creates <span ...>)

    s = s.replace(/("([^"\\]|\\.)*")/g, `<span class="str">$1</span>`);

    // 3) never match inside HTML tags
    // (?![^<]*>) prevents matching things like class="cmt"
    const notInTag = '(?![^<]*>)';

    // booleans
    s = s.replace(new RegExp(`\\b(true|false)\\b${notInTag}`, 'g'), `<span class="bool">$1</span>`);

    // numbers
    s = s.replace(new RegExp(`\\b(\\d+)\\b${notInTag}`, 'g'), `<span class="num">$1</span>`);

    // keywords
    const kw = [
      'byte', 'short', 'int', 'long', 'float', 'double', 'char', 'boolean',
      'String', 'var', 'final', 'new', 'return', 'void',
      'public', 'private', 'protected', 'static'
       // 'class'
    ];
    const kwRe = new RegExp(`\\b(${kw.join('|')})\\b${notInTag}`, 'g');
    s = s.replace(kwRe, `<span class="kw">$1</span>`);
    s = s.replace(/(\/\/.*?$)/gm, `<span class="cmt">$1</span>`);

    return this.sanitizer.bypassSecurityTrustHtml(s);
  }



}