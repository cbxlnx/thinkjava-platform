export type Checkpoint = 'fundamentals' | 'loops' | 'arrays' | 'methods' | 'oop';

export type LearnTier = 'Beginner' | 'Intermediate' | 'Advanced';

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
    estimatedMinutes: number;
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