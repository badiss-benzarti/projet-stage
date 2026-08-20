export type InternshipType = 'PFE' | 'ETE';

export type InternshipStatus =
  | 'DRAFT'
  | 'SUBMITTED'
  | 'UNDER_REVIEW'
  | 'APPROVED'
  | 'REJECTED'
  | 'COMPANY_PENDING'
  | 'ACCEPTED'
  | 'REFUSED'
  | 'IN_PROGRESS'
  | 'COMPLETED';

/** Une transition que le role courant peut declencher, calculee par le backend. */
export interface AvailableAction {
  readonly target: InternshipStatus;
  readonly label: string;
  readonly requiresReason: boolean;
}

export interface HistoryEntry {
  readonly fromStatus: InternshipStatus | null;
  readonly toStatus: InternshipStatus;
  readonly actorName: string;
  readonly actorRole: string;
  readonly comment: string | null;
  readonly at: string;
}

export interface Internship {
  readonly id: number;
  readonly studentId: number;
  readonly studentName: string;
  readonly studentEmail: string;
  readonly type: InternshipType;
  readonly title: string;
  readonly description: string | null;
  readonly academicYear: string;
  readonly companyId: number | null;
  readonly companyName: string | null;
  readonly supervisorId: number | null;
  readonly supervisorName: string | null;
  readonly startDate: string | null;
  readonly endDate: string | null;
  readonly status: InternshipStatus;
  readonly rejectionReason: string | null;
  readonly availableActions: readonly AvailableAction[];
  readonly history: readonly HistoryEntry[];
}

export interface InternshipRequest {
  readonly type: InternshipType;
  readonly title: string;
  readonly description: string | null;
  readonly academicYear: string;
  readonly companyId: number | null;
  readonly companyName: string | null;
  readonly startDate: string | null;
  readonly endDate: string | null;
}

export interface TransitionRequest {
  readonly target: InternshipStatus;
  readonly comment?: string;
  readonly supervisorId?: number;
  readonly supervisorName?: string;
}

export interface Page<T> {
  readonly content: readonly T[];
  readonly totalElements: number;
  readonly totalPages: number;
  readonly number: number;
  readonly size: number;
}

/** Libelle et couleur de chaque etat. La couleur porte l'information. */
export const STATUS_META: Readonly<
  Record<InternshipStatus, { label: string; tone: 'neutral' | 'info' | 'warn' | 'ok' | 'bad' }>
> = {
  DRAFT:           { label: 'Brouillon',              tone: 'neutral' },
  SUBMITTED:       { label: 'Soumise',                tone: 'info' },
  UNDER_REVIEW:    { label: 'En cours d’examen',      tone: 'warn' },
  APPROVED:        { label: 'Approuvée',              tone: 'ok' },
  REJECTED:        { label: 'Refusée',                tone: 'bad' },
  COMPANY_PENDING: { label: 'En attente entreprise',  tone: 'warn' },
  ACCEPTED:        { label: 'Acceptée',               tone: 'ok' },
  REFUSED:         { label: 'Refusée par l’entreprise', tone: 'bad' },
  IN_PROGRESS:     { label: 'En cours',               tone: 'info' },
  COMPLETED:       { label: 'Terminé',                tone: 'ok' },
};

// --- Demandes de convention et de lettre d'affectation ---

export type RequestType = 'CONVENTION' | 'LETTRE_AFFECTATION';
export type RequestStatus = 'PENDING' | 'ISSUED' | 'REJECTED';

export interface DocumentRequest {
  readonly id: number;
  readonly internshipId: number;
  readonly type: RequestType;
  readonly status: RequestStatus;
  readonly reason: string | null;
  readonly processedBy: string | null;
  readonly createdAt: string;
}

export const REQUEST_TYPE_LABELS: Readonly<Record<RequestType, string>> = {
  CONVENTION: 'Convention de stage',
  LETTRE_AFFECTATION: 'Lettre d’affectation',
};

export const REQUEST_STATUS_LABELS: Readonly<Record<RequestStatus, string>> = {
  PENDING: 'En attente',
  ISSUED: 'Éditée',
  REJECTED: 'Refusée',
};
