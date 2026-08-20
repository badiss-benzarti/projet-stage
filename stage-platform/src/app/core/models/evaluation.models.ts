export type TaskStatus = 'PENDING' | 'VALIDATED' | 'REJECTED';
export type EvaluationStatus = 'DRAFT' | 'SUBMITTED';
export type ClaimStatus = 'OPEN' | 'IN_REVIEW' | 'RESPONDED' | 'REOPENED' | 'CLOSED';
export type ClaimType = 'NOTE' | 'TACHE' | 'AUTRE';

export interface Task {
  readonly id: number;
  readonly internshipId: number;
  readonly taskDate: string;
  readonly title: string;
  readonly description: string | null;
  readonly hours: number;
  readonly status: TaskStatus;
  readonly rejectionReason: string | null;
  readonly validatedBy: string | null;
  readonly createdAt: string;
}

export interface TaskRequest {
  readonly taskDate: string;
  readonly title: string;
  readonly description: string | null;
  readonly hours: number;
}

export interface TaskSummary {
  readonly total: number;
  readonly pending: number;
  readonly validated: number;
  readonly rejected: number;
  readonly validatedHours: number;
  readonly lastEntry: string | null;
}

export interface CriterionBreakdown {
  readonly note: number | null;
  readonly sur: number;
  readonly poids: string;
  readonly contribution: number | null;
}

export interface Evaluation {
  readonly id: number;
  readonly internshipId: number;
  readonly studentName: string;
  readonly supervisorName: string | null;
  readonly companyName: string | null;
  readonly internshipType: string | null;
  readonly technicalScore: number | null;
  readonly qualityScore: number | null;
  readonly autonomyScore: number | null;
  readonly communicationScore: number | null;
  readonly punctualityScore: number | null;
  readonly globalComment: string | null;
  readonly remarks: string | null;
  readonly finalScore: number | null;
  readonly status: EvaluationStatus;
  readonly breakdown: Readonly<Record<string, CriterionBreakdown>>;
}

export interface EvaluationRequest {
  readonly technicalScore: number;
  readonly qualityScore: number;
  readonly autonomyScore: number;
  readonly communicationScore: number;
  readonly punctualityScore: number;
  readonly globalComment: string | null;
  readonly remarks: string | null;
}

export interface ClaimMessage {
  readonly id: number;
  readonly authorName: string;
  readonly authorRole: string;
  readonly content: string;
  readonly at: string;
}

export interface Claim {
  readonly id: number;
  readonly internshipId: number;
  readonly studentName: string;
  readonly type: ClaimType;
  readonly subject: string;
  readonly status: ClaimStatus;
  readonly reopenCount: number;
  readonly createdAt: string;
  readonly closedAt: string | null;
  readonly messages: readonly ClaimMessage[];
}

export interface RiskAssessment {
  readonly internshipId: number;
  readonly studentName: string;
  readonly risk: 'LOW' | 'MEDIUM' | 'HIGH' | 'UNAVAILABLE';
  readonly probability: number | null;
  readonly probabilities: Readonly<Record<string, number>>;
  readonly drivers: readonly string[];
}

export const TASK_STATUS_META: Readonly<
  Record<TaskStatus, { label: string; tone: 'ok' | 'warn' | 'bad' }>
> = {
  PENDING: { label: 'En attente', tone: 'warn' },
  VALIDATED: { label: 'Validée', tone: 'ok' },
  REJECTED: { label: 'Refusée', tone: 'bad' },
};

export const CLAIM_STATUS_LABELS: Readonly<Record<ClaimStatus, string>> = {
  OPEN: 'Ouverte',
  IN_REVIEW: 'En cours d’examen',
  RESPONDED: 'Réponse apportée',
  REOPENED: 'Relancée',
  CLOSED: 'Close',
};
