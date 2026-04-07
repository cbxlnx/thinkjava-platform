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

type ReviewItem = {
  id: number;
  prompt: string;
  checkpoint: Checkpoint;
  difficulty: Difficulty;
  selectedAnswer: AnswerOrSkip | null;
  correctAnswer: AnswerOption;
  selectedText: string;
  correctText: string;
  isSkipped: boolean;
};

// runs the diagnostic from intro to result
// builds a balanced quiz across checkpoints
// adapts difficulty based on learner performance
// stops early when enough placement evidence exists
// sends the computed result to the backend
@Component({
  selector: 'app-diagnostic-page',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './diagnostic-page.component.html',
  styleUrls: ['./diagnostic-page.component.css'],
})
export class DiagnosticPageComponent {
  // current screen state
  step: Step = 'INTRO';

  // full question bank and current run selection
  all: Question[] = DIAGNOSTIC_QUESTIONS;
  selectedPool: Question[] = [];

  // checkpoint traversal state
  checkpoints: Checkpoint[] = [...CHECKPOINTS];
  currentCheckpointIndex = 0;

  // per-checkpoint difficulty and performance tracking
  currentDifficulty: Difficulty = 1;
  askedThisCheckpoint = 0;
  correctThisCheckpoint = 0;
  wrongStreakBeginner = 0;

  // overall quiz performance tracking
  globalAnswered = 0;
  globalCorrect = 0;
  globalWrongStreak = 0;

  // current question and answers
  currentQuestion: Question | null = null;
  selected: AnswerOption | null = null;
  answers: Record<number, AnswerOrSkip> = {};

  // result and review state
  reviewItems: ReviewItem[] = [];
  showReview = false;
  answeredCount = 0;
  totalQuestionsPlanned = MAX_QUESTIONS;
  resultJson: any = null;

  // placement guardrails
  private readonly MIN_ADVANCED_IN_QUIZ = 2;
  private readonly MIN_ADVANCED_ATTEMPTED_FOR_ADVANCED = 1;

  constructor(private diagnostic: DiagnosticService, private router: Router) {}

  // ---------------------------
  // diagnostic lifecycle
  // ---------------------------
  start() {
    // move into quiz mode
    this.step = 'QUIZ';
    this.answers = {};
    this.resultJson = null;

    // reset overall counters
    this.answeredCount = 0;
    this.globalAnswered = 0;
    this.globalCorrect = 0;
    this.globalWrongStreak = 0;

    // restart from the first checkpoint
    this.currentCheckpointIndex = 0;

    // clear result-only state
    this.reviewItems = [];
    this.showReview = false;
    this.resetCheckpointState();

    // build a balanced quiz for this run
    this.selectedPool = this.buildSelectedPool();

    // cap the planned total to what was actually selected
    this.totalQuestionsPlanned = Math.min(MAX_QUESTIONS, this.selectedPool.length);

    this.loadNextQuestion();
  }

  retake() {
    // return to the intro screen
    this.step = 'INTRO';
    this.currentQuestion = null;
    this.selected = null;
    this.answers = {};
    this.selectedPool = [];
    this.resultJson = null;

    this.answeredCount = 0;
    this.totalQuestionsPlanned = MAX_QUESTIONS;
    this.currentCheckpointIndex = 0;

    // clear review and result data
    this.reviewItems = [];
    this.showReview = false;
    this.resetCheckpointState();
  }

  exitDiagnostic() {
    // protect in-progress quiz state
    if (this.step === 'QUIZ') {
      const confirmed = window.confirm(
        'Are you sure you want to leave the assessment? Your current progress will be lost.'
      );
      if (!confirmed) return;
    }

    this.router.navigate(['/dashboard']);
  }

  private resetCheckpointState() {
    // restart checkpoint progression at beginner level
    this.currentDifficulty = 1;
    this.askedThisCheckpoint = 0;
    this.correctThisCheckpoint = 0;
    this.wrongStreakBeginner = 0;
  }

  // ---------------------------
  // ui getters
  // ---------------------------
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
    return Math.min(this.answeredCount + 1, this.totalQuestionsPlanned);
  }

  get isLastQuestion(): boolean {
    return this.questionNumber >= this.totalQuestionsPlanned;
  }

  get progressPercent(): number {
    if (!this.totalQuestionsPlanned) return 0;
    return Math.min(100, (this.answeredCount / this.totalQuestionsPlanned) * 100);
  }

  // result summary values
  get completionPercent(): number {
    return this.scorePercent;
  }

  get scorePercent(): number {
    const attempted = Object.keys(this.answers).length;
    if (!attempted) return 0;

    // count only correct non-skip answers
    let correct = 0;

    for (const q of this.selectedPool) {
      const given = this.answers[q.id];
      if (given !== 'SKIP' && given === q.correctOption) {
        correct++;
      }
    }

    return Math.round((correct / attempted) * 100);
  }

  get overallLevel(): string {
    const p = this.scorePercent;

    // only allow advanced if advanced questions were actually attempted
    const advAttempted = this.advancedAttemptedCount();
    const canBeAdvanced = advAttempted >= this.MIN_ADVANCED_ATTEMPTED_FOR_ADVANCED;

    if (p >= 75 && canBeAdvanced) return 'Advanced';
    if (p >= 45) return 'Intermediate';
    return 'Beginner';
  }

  // result detail lists
  get strengths(): string[] {
    if (!this.resultJson) return [];

    // surface the strongest checkpoints
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

    // map backend checkpoint keys to friendly labels
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

    // weak or unknown areas take priority
    const weak = order.filter((k) => levels[k] === 'Weak' || levels[k] === 'Unknown');
    if (weak.length) return weak.map((k) => label[k]);

    // otherwise show the best medium-level improvement targets
    const medium = order.filter((k) => levels[k] === 'Medium');
    if (medium.length) return medium.slice(0, 2).map((k) => label[k]);

    return [];
  }

  // ---------------------------
  // quiz interactions
  // ---------------------------
  pick(option: AnswerOption) {
    // keep the current selection in sync with the ui
    this.selected = option;
    if (!this.currentQuestion) return;
    this.answers[this.currentQuestion.id] = option;
  }

  next() {
    if (!this.currentQuestion || !this.selected) return;

    // persist the chosen answer before scoring it
    this.answers[this.currentQuestion.id] = this.selected;

    const isCorrect = this.selected === this.currentQuestion.correctOption;
    this.processAnswer(isCorrect);
  }

  skipQuestion() {
    if (!this.currentQuestion) return;

    // record the skip but still count it as an attempted question
    this.answers[this.currentQuestion.id] = 'SKIP';
    this.selected = null;

    this.processAnswer(false);
  }

  // ---------------------------
  // quiz progression engine
  // ---------------------------
  private processAnswer(isCorrect: boolean) {
    // update global progress used by the ui and stopping rules
    this.answeredCount++;
    this.globalAnswered++;

    if (isCorrect) {
      this.globalCorrect++;
      this.globalWrongStreak = 0;
    } else {
      this.globalWrongStreak++;
    }

    // update per-checkpoint state
    this.askedThisCheckpoint++;
    if (isCorrect) {
      this.correctThisCheckpoint++;
      if (this.currentDifficulty === 1) this.wrongStreakBeginner = 0;
    } else if (this.currentDifficulty === 1) {
      // track repeated misses at the easiest level
      this.wrongStreakBeginner++;
    }

    // stop when the planned quiz size has been reached
    if (this.answeredCount >= this.totalQuestionsPlanned) {
      this.submit();
      return;
    }

    // stop early when performance clearly points to beginner placement
    const globalAccuracy = this.globalAnswered ? this.globalCorrect / this.globalAnswered : 0;
    if (this.globalAnswered >= 5 && (globalAccuracy < 0.25 || this.globalWrongStreak >= 4)) {
      this.finishAndSubmitEarly();
      return;
    }

    // move on if beginner questions in this checkpoint are repeatedly missed
    if (this.currentDifficulty === 1 && this.wrongStreakBeginner >= 2) {
      this.moveToNextCheckpoint();
      return;
    }

    // promote difficulty inside the checkpoint when performance is strong enough
    if (this.askedThisCheckpoint >= 2) {
      // use recent checkpoint performance to decide whether to escalate
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

    // prefer the current difficulty, but fall back to any unanswered question in the checkpoint
    const candidates = this.selectedPool
      .filter((q) => q.checkpoint === cp && this.answers[q.id] === undefined)
      .sort((a, b) => a.difficulty - b.difficulty);

    const nextQ = candidates.find((q) => q.difficulty === this.currentDifficulty) ?? candidates[0];

    if (!nextQ) {
      // no more questions remain for this checkpoint
      this.moveToNextCheckpoint();
      return;
    }

    // keep the state aligned with the actual question being shown
    this.currentDifficulty = nextQ.difficulty;
    this.currentQuestion = nextQ;
    this.selected = null;
  }

  private moveToNextCheckpoint() {
    // advance to the next topic area
    this.currentCheckpointIndex++;

    if (this.currentCheckpointIndex >= this.checkpoints.length) {
      // submit once every checkpoint has been processed
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
  // submission and scoring
  // ---------------------------
  submit() {
    // build per-checkpoint scoring data from attempted questions
    const stats: Record<string, { correct: number; total: number }> = {};

    for (const q of this.selectedPool) {
      const given = this.answers[q.id];
      if (given === undefined) continue;

      if (!stats[q.checkpoint]) stats[q.checkpoint] = { correct: 0, total: 0 };
      stats[q.checkpoint].total++;

      if (given !== 'SKIP' && given === q.correctOption) {
        stats[q.checkpoint].correct++;
      }
    }

    // convert checkpoint accuracy into mastery labels
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
      diagnosticPercent: this.scorePercent,
    };

    // persist the result and show the result screen
    this.diagnostic.complete(payload).subscribe({
      next: (res) => {
        this.resultJson = res;
        this.reviewItems = this.buildReviewItems();
        this.showReview = false;
        this.step = 'RESULT';
      },
      error: () => {
        this.resultJson = { ...payload, error: 'Failed to save result' };
        this.reviewItems = this.buildReviewItems();
        this.showReview = false;
        this.step = 'RESULT';
      },
    });
  }
  // picks the most appropriate starting module based on checkpoint performance, with a bias towards earlier topics
  pickStartModule(levels: Record<string, string>) {
    const order: Checkpoint[] = ['fundamentals', 'loops', 'arrays', 'methods', 'oop'];

    // prioritize the first weak checkpoint
    const firstWeak = order.find((k) => levels[k] === 'Weak' || levels[k] === 'Unknown');
    if (firstWeak) return firstWeak;

    // otherwise start from the first medium checkpoint
    const firstMedium = order.find((k) => levels[k] === 'Medium');
    if (firstMedium) return firstMedium;

    return 'fundamentals';
  }

  // ---------------------------
  // result actions
  // ---------------------------
  viewPath() {
    // send the learner into the next main flow
    this.router.navigate(['/dashboard']);
  }

  // ---------------------------
  // quiz selection helpers
  // ---------------------------
  private buildSelectedPool(): Question[] {
    // pick a checkpoint-balanced set first
    const picked: Question[] = [];

    for (const cp of this.checkpoints) {
      const quota = QUIZ_QUOTA[cp] ?? 0;
      const fromCp = this.all.filter((q) => q.checkpoint === cp);

      const cpPicked = this.pickWithDifficultyPreference(fromCp, quota);
      picked.push(...cpPicked);
    }

    const trimmed = this.shuffle([...picked]).slice(0, MAX_QUESTIONS);

    // make sure the final quiz still includes enough advanced coverage
    const enforced = this.enforceMinAdvanced(trimmed, this.MIN_ADVANCED_IN_QUIZ);

    return this.sortByCheckpointOrder(enforced);
  }

  private pickWithDifficultyPreference(pool: Question[], quota: number): Question[] {
    if (quota <= 0) return [];
    if (pool.length <= quota) return this.sortByDifficulty(pool);

    // split the pool into difficulty buckets
    const d1 = this.shuffle(pool.filter((q) => q.difficulty === 1));
    const d2 = this.shuffle(pool.filter((q) => q.difficulty === 2));
    const d3 = this.shuffle(pool.filter((q) => q.difficulty === 3));

    const picked: Question[] = [];

    // rotate through the difficulty buckets to keep coverage balanced
    const buckets: Question[][] = [d1, d2, d3];
    let idx = 0;

    while (picked.length < quota) {
      const bucket = buckets[idx % buckets.length];
      const next = bucket.shift();
      if (next) picked.push(next);

      idx++;
      // stop once every bucket has been exhausted
      if (buckets.every((b) => b.length === 0)) break;
    }

    return this.sortByDifficulty(picked).slice(0, quota);
  }

  private sortByDifficulty(list: Question[]): Question[] {
    // keep easier questions first inside a checkpoint
    return [...list].sort((a, b) => a.difficulty - b.difficulty);
  }

  private sortByCheckpointOrder(list: Question[]): Question[] {
    // preserve the intended checkpoint progression order
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
  // Fisher-Yates shuffle implementation to randomize question order
  private shuffle<T>(arr: T[]): T[] {
    // randomize selections to vary each run
    const a = [...arr];
    for (let i = a.length - 1; i > 0; i--) {
      const j = Math.floor(Math.random() * (i + 1));
      [a[i], a[j]] = [a[j], a[i]];
    }
    return a;
  }
  // ensures the quiz includes a minimum number of advanced questions by swapping in from the full pool if needed
  private enforceMinAdvanced(picked: Question[], minAdvanced: number): Question[] {
    const currentAdvanced = picked.filter((q) => q.difficulty === 3).length;
    if (currentAdvanced >= minAdvanced) return picked;

    // work out how many advanced questions still need to be added
    const need = minAdvanced - currentAdvanced;

    // add advanced questions that are not already in the selected set
    const advancedCandidates = this.all.filter(
      (q) => q.difficulty === 3 && !picked.some((p) => p.id === q.id)
    );

    if (advancedCandidates.length === 0) return picked;

    const add = this.shuffle(advancedCandidates).slice(0, need);

    // replace beginner questions first so the quiz length stays stable
    const replaceable = picked
      .map((q, idx) => ({ q, idx }))
      .filter((x) => x.q.difficulty === 1)
      .map((x) => x.idx);

    const out = [...picked];

    for (let i = 0; i < add.length; i++) {
      const idx = replaceable[i];
      if (idx === undefined) break;
      // swap in an advanced question
      out[idx] = add[i];
    }

    return out;
  }

  private advancedAttemptedCount(): number {
    // count advanced questions that the learner actually interacted with
    return this.selectedPool.filter(
      (q) => q.difficulty === 3 && this.answers[q.id] !== undefined
    ).length;
  }

  // ---------------------------
  // review helpers
  // ---------------------------
  private buildReviewItems(): ReviewItem[] {
    // include only missed or skipped questions in the review
    return this.selectedPool
      .filter((q) => {
        const given = this.answers[q.id];
        return given !== undefined && given !== q.correctOption;
      })
      .map((q) => {
        const given = this.answers[q.id];

        return {
          id: q.id,
          prompt: q.prompt,
          checkpoint: q.checkpoint,
          difficulty: q.difficulty,
          selectedAnswer: given ?? null,
          correctAnswer: q.correctOption,
          selectedText: this.getAnswerText(q, given),
          correctText: this.getAnswerText(q, q.correctOption),
          isSkipped: given === 'SKIP',
        };
      });
  }

  private getAnswerText(q: Question, answer: AnswerOption | 'SKIP' | undefined): string {
    if (!answer) return 'No answer';
    if (answer === 'SKIP') return 'Skipped';

    // map the option key back to its visible text
    const optionMap: Record<AnswerOption, string> = {
      A: q.optionA,
      B: q.optionB,
      C: q.optionC,
      D: q.optionD,
    };

    return `${answer}: ${optionMap[answer]}`;
  }
}

// helper type for checkpoint ordering
type CheckointSafe = 'fundamentals' | 'loops' | 'arrays' | 'methods' | 'oop';
