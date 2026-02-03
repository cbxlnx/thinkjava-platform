import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
import { DiagnosticService } from '../../core/diagnostic/diagnostic.service';
import {
  DIAGNOSTIC_QUESTIONS,
  Question,
  Checkpoint,
  Difficulty,
  AnswerOption,
  MAX_QUESTIONS,
  QUIZ_QUOTA,
  CHECKPOINTS,
} from './questions.mock';

type Step = 'INTRO' | 'QUIZ' | 'RESULT';
type AnswerOrSkip = AnswerOption | 'SKIP';

// selects a balanced set of questions
// adapts difficulty within each topic
// skips topics if the learner struggles
// stops early if performance is very low
// computes mastery per topic
// sends results to the backend
@Component({
  selector: 'app-diagnostic-page',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './diagnostic-page.component.html',
  styleUrls: ['./diagnostic-page.component.css'],
})

export class DiagnosticPageComponent {
  step: Step = 'INTRO';

  // full pool 
  all: Question[] = DIAGNOSTIC_QUESTIONS;

  // these are selected per run 
  selectedPool: Question[] = [];

  checkpoints: Checkpoint[] = [...CHECKPOINTS];
  currentCheckpointIndex = 0;

  currentDifficulty: Difficulty = 1;
  askedThisCheckpoint = 0;
  correctThisCheckpoint = 0;
  wrongStreakBeginner = 0;

  globalAnswered = 0;
  globalCorrect = 0;
  globalWrongStreak = 0;

  currentQuestion: Question | null = null;
  selected: AnswerOption | null = null;

  answers: Record<number, AnswerOrSkip> = {};

  answeredCount = 0;
  totalQuestionsPlanned = MAX_QUESTIONS;

  resultJson: any = null;

  private readonly MIN_ADVANCED_IN_QUIZ = 2;
  private readonly MIN_ADVANCED_ATTEMPTED_FOR_ADVANCED = 1;


  constructor(private diagnostic: DiagnosticService, private router: Router) { }

  // START / RESET
  start() {
    this.step = 'QUIZ';
    this.answers = {};
    this.resultJson = null;

    this.answeredCount = 0;
    this.globalAnswered = 0;
    this.globalCorrect = 0;
    this.globalWrongStreak = 0;

    this.currentCheckpointIndex = 0;
    this.resetCheckpointState();

    // build the quiz: proportioned selection (2 per topic by default)
    this.selectedPool = this.buildSelectedPool();

    // real total = how many we managed to select
    this.totalQuestionsPlanned = Math.min(MAX_QUESTIONS, this.selectedPool.length);

    this.loadNextQuestion();
  }

  private resetCheckpointState() {
    this.currentDifficulty = 1;
    this.askedThisCheckpoint = 0;
    this.correctThisCheckpoint = 0;
    this.wrongStreakBeginner = 0;
  }

  // UI HELPERS
  get checkpointLabel() {
    const cp = this.currentQuestion?.checkpoint;
    if (!cp) return '';
    return cp.charAt(0).toUpperCase() + cp.slice(1);
  }

  get difficultyLabel() {
    const d = this.currentQuestion?.difficulty;
    if (d === 1) return 'Beginner';
    if (d === 2) return 'Intermediate';
    if (d === 3) return 'Advanced';
    return '';
  }

  get questionNumber(): number {
    // on question 1 (answeredCount=0) => 1
    return Math.min(this.answeredCount + 1, this.totalQuestionsPlanned);
  }

  get isLastQuestion(): boolean {
    return this.questionNumber >= this.totalQuestionsPlanned;
  }

  get progressPercent(): number {
    // progress bar should be 0% at Q1, then grow after answering
    if (!this.totalQuestionsPlanned) return 0;
    return Math.min(100, (this.answeredCount / this.totalQuestionsPlanned) * 100);
  }

  // result UI values
  get completionPercent(): number {
    // this is the big ring percent show score
    return this.scorePercent;
  }

  get scorePercent(): number {
    if (!this.resultJson) return 0;
    const map: Record<string, number> = { Strong: 1, Medium: 0.66, Weak: 0, Unknown: 0 };

    const keys: Checkpoint[] = ['fundamentals', 'loops', 'arrays', 'methods', 'oop'];
    const vals = keys.map((k) => map[this.resultJson?.[k]] ?? 0);

    const avg = vals.reduce((a, b) => a + b, 0) / vals.length;
    return Math.round(avg * 100);
  }

  get overallLevel(): string {
    const p = this.scorePercent;

    const advAttempted = this.advancedAttemptedCount();
    const canBeAdvanced = advAttempted >= this.MIN_ADVANCED_ATTEMPTED_FOR_ADVANCED;

    if (p >= 75 && canBeAdvanced) return 'Advanced';
    if (p >= 45) return 'Intermediate';
    return 'Beginner';
  }


  get strengths(): string[] {
    if (!this.resultJson) return [];
    const list: string[] = [];

    if (this.resultJson.fundamentals === 'Strong') list.push('Syntax');
    if (this.resultJson.loops === 'Strong') list.push('Loops');
    if (this.resultJson.arrays === 'Strong') list.push('Arrays');
    if (this.resultJson.methods === 'Strong') list.push('Methods');
    if (this.resultJson.oop === 'Strong') list.push('Classes');

    return list;
  }

  get needsImprovement(): string[] {
    if (!this.resultJson) return [];

    const label: Record<Checkpoint, string> = {
      fundamentals: 'Syntax',
      loops: 'Loops',
      arrays: 'Arrays',
      methods: 'Methods',
      oop: 'OOP Concepts',
    };

    const levels: Record<Checkpoint, string> = {
      fundamentals: this.resultJson.fundamentals,
      loops: this.resultJson.loops,
      arrays: this.resultJson.arrays,
      methods: this.resultJson.methods,
      oop: this.resultJson.oop,
    };

    const order: Checkpoint[] = ['fundamentals', 'loops', 'arrays', 'methods', 'oop'];

    // 1) weak / unknown first
    const weak = order.filter((k) => levels[k] === 'Weak' || levels[k] === 'Unknown');
    if (weak.length) return weak.map((k) => label[k]);

    // 2) if no weak, recommend medium topics (pick top 1–2 so it doesn't look noisy)
    const medium = order.filter((k) => levels[k] === 'Medium');
    if (medium.length) return medium.slice(0, 2).map((k) => label[k]);

    // 3) all Strong => allow "Looks solid"
    return [];
  }


  // ---------------------------
  // QUIZ ENGINE
  // ---------------------------
  pick(option: AnswerOption) {
    this.selected = option;
    if (!this.currentQuestion) return;
    this.answers[this.currentQuestion.id] = option;
  }

  next() {
    if (!this.currentQuestion || !this.selected) return;
    this.answers[this.currentQuestion.id] = this.selected;

    const isCorrect = this.selected === this.currentQuestion.correctOption;
    this.processAnswer(isCorrect);
  }

  private processAnswer(isCorrect: boolean) {
    // counts for UI / stopping
    this.answeredCount++;

    this.globalAnswered++;
    if (isCorrect) {
      this.globalCorrect++;
      this.globalWrongStreak = 0;
    } else {
      this.globalWrongStreak++;
    }

    // per-checkpoint tracking
    this.askedThisCheckpoint++;
    if (isCorrect) {
      this.correctThisCheckpoint++;
      if (this.currentDifficulty === 1) this.wrongStreakBeginner = 0;
    } else {
      if (this.currentDifficulty === 1) this.wrongStreakBeginner++;
    }

    // stop if we already answered planned amount
    if (this.answeredCount >= this.totalQuestionsPlanned) {
      this.submit();
      return;
    }

    // GLOBAL early stop:  prevents a struggling learner from being forced through all 10 questions 
    // when the system already has enough evidence to place them at a beginner level
    const globalAccuracy = this.globalAnswered ? this.globalCorrect / this.globalAnswered : 0;
    if (this.globalAnswered >= 5 && (globalAccuracy < 0.25 || this.globalWrongStreak >= 4)) {
      this.finishAndSubmitEarly();
      return;
    }

    // PER-CHECKPOINT early stop: 2 beginner wrong/skip in a row
    //if a learner cannot answer beginner level questions the system should not escalate difficulty
    if (this.currentDifficulty === 1 && this.wrongStreakBeginner >= 2) {
      this.moveToNextCheckpoint();
      return;
    }

    // difficulty progression within checkpoint
    if (this.askedThisCheckpoint >= 2) {
      const ratio = this.correctThisCheckpoint / this.askedThisCheckpoint;

      if (ratio >= 0.6 && this.currentDifficulty < 3) {
        this.currentDifficulty = (this.currentDifficulty + 1) as Difficulty;
        this.askedThisCheckpoint = 0;
        this.correctThisCheckpoint = 0;
        this.wrongStreakBeginner = 0;
      } else {
        this.moveToNextCheckpoint();
        return;
      }
    }

    this.loadNextQuestion();
  }


  private loadNextQuestion() {
    const cp = this.checkpoints[this.currentCheckpointIndex];

    // find the next unanswered question in this checkpoint, preferring lowest difficulty available
    const candidates = this.selectedPool
      .filter((q) => q.checkpoint === cp && this.answers[q.id] === undefined)
      .sort((a, b) => a.difficulty - b.difficulty);

    const nextQ = candidates.find((q) => q.difficulty === this.currentDifficulty) ?? candidates[0];

    if (!nextQ) {
      this.moveToNextCheckpoint();
      return;
    }

    // align currentDifficulty with what we're actually showing
    this.currentDifficulty = nextQ.difficulty;

    this.currentQuestion = nextQ;
    this.selected = null;
  }

  private moveToNextCheckpoint() {
    this.currentCheckpointIndex++;

    if (this.currentCheckpointIndex >= this.checkpoints.length) {
      this.submit();
      return;
    }

    this.resetCheckpointState();
    this.loadNextQuestion();
  }

  private finishAndSubmitEarly() {
    this.submit();
  }

  // ---------------------------
  // SUBMIT / SCORE
  // ---------------------------
  submit() {
    const stats: Record<string, { correct: number; total: number }> = {};

    // score only answered questions from selected pool (including SKIP)
    for (const q of this.selectedPool) {
      const given = this.answers[q.id];
      if (given === undefined) continue; // not answered at all

      if (!stats[q.checkpoint]) stats[q.checkpoint] = { correct: 0, total: 0 };
      stats[q.checkpoint].total++;

      // SKIP counts as incorrect, so only count correct when it's not SKIP and matches
      if (given !== 'SKIP' && given === q.correctOption) {
        stats[q.checkpoint].correct++;
      }
    }

    const level = (cp: string) => {
      const s = stats[cp];
      if (!s || s.total === 0) return 'Unknown';
      const r = s.correct / s.total;

      if (r >= 0.8) return 'Strong';
      if (r >= 0.45) return 'Medium';
      return 'Weak';
    };

    const levels = {
      fundamentals: level('fundamentals'),
      loops: level('loops'),
      arrays: level('arrays'),
      methods: level('methods'),
      oop: level('oop'),
    };

    const payload = {
      ...levels,
      startModule: this.pickStartModule(levels),
    };

    this.diagnostic.complete(payload).subscribe({
      next: (res) => {
        this.resultJson = res;
        this.step = 'RESULT';
      },
      error: () => {
        // show something still
        this.resultJson = { ...payload, error: 'Failed to save result' };
        this.step = 'RESULT';
      },
    });
  }


  pickStartModule(levels: Record<string, string>) {
    const order: Checkpoint[] = ['fundamentals', 'loops', 'arrays', 'methods', 'oop'];

    const firstWeak = order.find((k) => levels[k] === 'Weak' || levels[k] === 'Unknown');
    if (firstWeak) return firstWeak;

    const firstMedium = order.find((k) => levels[k] === 'Medium');
    if (firstMedium) return firstMedium;

    return 'fundamentals'; // all Strong - doesn't really matter
  }


  // ---------------------------
  // RESULT BUTTONS
  // ---------------------------
  viewPath() {
    this.router.navigate(['/dashboard']);
  }

  retake() {
    this.step = 'INTRO';
    this.currentQuestion = null;
    this.selected = null;
    this.answers = {};
    this.selectedPool = [];
    this.resultJson = null;

    this.answeredCount = 0;
    this.totalQuestionsPlanned = MAX_QUESTIONS;

    this.currentCheckpointIndex = 0;
    this.resetCheckpointState();
  }

  // ---------------------------
  // QUIZ SELECTION
  // ---------------------------
  private buildSelectedPool(): Question[] {
    const picked: Question[] = [];

    for (const cp of this.checkpoints) {
      const quota = QUIZ_QUOTA[cp] ?? 0;
      const fromCp = this.all.filter((q) => q.checkpoint === cp);

      const cpPicked = this.pickWithDifficultyPreference(fromCp, quota);
      picked.push(...cpPicked);
    }

    const trimmed = this.shuffle([...picked]).slice(0, MAX_QUESTIONS);

    // guarantee at least some advanced questions exist
    const enforced = this.enforceMinAdvanced(trimmed, this.MIN_ADVANCED_IN_QUIZ);

    return this.sortByCheckpointOrder(enforced);
  }


  private pickWithDifficultyPreference(pool: Question[], quota: number): Question[] {
    if (quota <= 0) return [];
    if (pool.length <= quota) return this.sortByDifficulty(pool);

    const d1 = this.shuffle(pool.filter((q) => q.difficulty === 1));
    const d2 = this.shuffle(pool.filter((q) => q.difficulty === 2));
    const d3 = this.shuffle(pool.filter((q) => q.difficulty === 3));

    const picked: Question[] = [];

    // ladder order: 1 -> 2 -> 3 (repeat if needed)
    const buckets: Question[][] = [d1, d2, d3];
    let idx = 0;

    while (picked.length < quota) {
      const bucket = buckets[idx % buckets.length];
      const next = bucket.shift();
      if (next) picked.push(next);

      idx++;

      // if all buckets empty, stop
      if (buckets.every((b) => b.length === 0)) break;
    }

    return this.sortByDifficulty(picked).slice(0, quota);
  }

  private sortByDifficulty(list: Question[]): Question[] {
    return [...list].sort((a, b) => a.difficulty - b.difficulty);
  }

  private sortByCheckpointOrder(list: Question[]): Question[] {
    const orderIndex: Record<CheckointSafe, number> = {
      fundamentals: 0,
      loops: 1,
      arrays: 2,
      methods: 3,
      oop: 4,
    };

    return [...list].sort((a, b) => {
      const ca = orderIndex[a.checkpoint as CheckointSafe] ?? 999;
      const cb = orderIndex[b.checkpoint as CheckointSafe] ?? 999;
      if (ca !== cb) return ca - cb;
      return a.difficulty - b.difficulty;
    });
  }

  private shuffle<T>(arr: T[]): T[] {
    const a = [...arr];
    for (let i = a.length - 1; i > 0; i--) {
      const j = Math.floor(Math.random() * (i + 1));
      [a[i], a[j]] = [a[j], a[i]];
    }
    return a;
  }

  skipQuestion() {
    if (!this.currentQuestion) return;

    // mark as answered, but incorrect
    this.answers[this.currentQuestion.id] = 'SKIP';
    this.selected = null;

    // reuse the same flow as next(), but treat as incorrect
    this.processAnswer(false);
  }

  private enforceMinAdvanced(picked: Question[], minAdvanced: number): Question[] {
    const currentAdvanced = picked.filter(q => q.difficulty === 3).length;
    if (currentAdvanced >= minAdvanced) return picked;

    const need = minAdvanced - currentAdvanced;

    const advancedCandidates = this.all.filter(
      q => q.difficulty === 3 && !picked.some(p => p.id === q.id)
    );

    if (advancedCandidates.length === 0) return picked;

    const add = this.shuffle(advancedCandidates).slice(0, need);

    // replace easiest questions first to keep length stable
    const replaceable = picked
      .map((q, idx) => ({ q, idx }))
      .filter(x => x.q.difficulty === 1)
      .map(x => x.idx);

    const out = [...picked];

    for (let i = 0; i < add.length; i++) {
      const idx = replaceable[i];
      if (idx === undefined) break;
      out[idx] = add[i];
    }

    return out;
  }

  private advancedAttemptedCount(): number {
    return this.selectedPool.filter(
      q => q.difficulty === 3 && this.answers[q.id] !== undefined
    ).length;
  }

}

// TS helper (narrowing)
type CheckointSafe = 'fundamentals' | 'loops' | 'arrays' | 'methods' | 'oop';