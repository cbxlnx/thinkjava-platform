export type Checkpoint = 'fundamentals' | 'loops' | 'arrays' | 'methods' | 'oop';

export type LearnTier = 'Beginner' | 'Intermediate' | 'Advanced';
export type LessonBlockType =
  | 'MARKDOWN'
  | 'VIDEO'
  | 'INLINE_QUIZ'
  // optional (if you start using them):
  | 'CONCEPT_SPLIT'
  | 'PRIMITIVES_GRID'
  | 'REFERENCE_CARDS'
  | 'CODE_WITH_TIPS'
  | 'CASTING_SPLIT'
  | 'PITFALLS_GRID';

export interface LessonResponse {
  lesson: {
    id: string;
    checkpoint: string;
    title: string;
    orderIndex: number;
    estimatedMinutes: number | null;
  };

  blocks: Array<{
    order: number;
    type: LessonBlockType;
    markdown?: string | null;
    videoTitle?: string | null;
    videoUrl?: string | null;
    payload?: string | null
  }>;

  quiz: {
    questions: Array<{
      id: string;
      prompt: string;
      options: string[];
    }>;
  };
}

export type LearnPathResponse = {
  recommendedLessonId: string;
  startCheckpoint: Checkpoint;
  mastery: Record<Checkpoint, number>;
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

// UI list item (frontend-derived)
export type LessonCard = {
  id: string;
  checkpoint: Checkpoint;
  title: string;
  orderIndex: number;
  estimatedMinutes: number;
  tier: LearnTier;
  locked: boolean;
  masteryPercent: number; // per-checkpoint mastery mapped to 0..100
};