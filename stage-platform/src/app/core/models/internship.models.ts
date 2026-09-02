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
  | 'COMPLETED'
  | 'ABANDONED';

/** Une transition que le role courant peut declencher, calculee par le backend. */
export interface AvailableAction {
  readonly target: InternshipStatus;
  readonly label: string;
  /** Phrase expliquant ce que l'action déclenche, affichée sous le bouton. */
  readonly hint: string;
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
  readonly companyAddress: string | null;
  readonly companyEmail: string | null;
  readonly companyPhone: string | null;
  readonly contactName: string | null;
  readonly contactEmail: string | null;
  readonly contactPhone: string | null;
  /** Encadrant propose par l etudiant a la demande, avant confirmation. */
  readonly requestedSupervisorId: number | null;
  readonly requestedSupervisorName: string | null;
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
  readonly companyAddress: string | null;
  readonly companyEmail: string | null;
  readonly companyPhone: string | null;
  readonly contactName: string | null;
  readonly contactEmail: string | null;
  readonly contactPhone: string | null;
  readonly requestedSupervisorId: number | null;
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
/**
 * Libellé et explication de chaque état.
 *
 * Le libellé tient dans une pastille, l'explication répond à la seule
 * question que se pose l'utilisateur : « et maintenant, qui doit agir ? ».
 * C'est pour cela que chaque phrase nomme explicitement celui dont on
 * attend quelque chose.
 */
export const STATUS_META: Readonly<
  Record<
    InternshipStatus,
    { label: string; description: string; tone: 'neutral' | 'info' | 'warn' | 'ok' | 'bad' }
  >
> = {
  DRAFT: {
    label: 'Brouillon',
    description: 'Votre demande n’est pas encore envoyée. Vous pouvez la modifier librement.',
    tone: 'neutral',
  },
  SUBMITTED: {
    label: 'Envoyée',
    description: 'Le service des stages a reçu votre demande. Il doit encore la prendre en charge.',
    tone: 'info',
  },
  UNDER_REVIEW: {
    label: 'En cours d’examen',
    description: 'Le service des stages examine votre dossier avant de le transmettre.',
    tone: 'warn',
  },
  APPROVED: {
    label: 'Validée par l’école',
    description: 'L’école a validé le dossier. Il part maintenant chez l’entreprise.',
    tone: 'ok',
  },
  REJECTED: {
    label: 'Refusée par l’école',
    description: 'Le service des stages a refusé le dossier. Le motif est indiqué.',
    tone: 'bad',
  },
  COMPANY_PENDING: {
    label: 'Chez l’entreprise',
    description: 'L’entreprise a reçu la demande. C’est à elle d’accepter ou de refuser.',
    tone: 'warn',
  },
  ACCEPTED: {
    label: 'Acceptée',
    description: 'L’entreprise vous prend en stage. Votre encadrant vous est affecté.',
    tone: 'ok',
  },
  REFUSED: {
    label: 'Refusée par l’entreprise',
    description: 'L’entreprise a décliné. Le motif est indiqué ; cherchez une autre entreprise.',
    tone: 'bad',
  },
  IN_PROGRESS: {
    label: 'Stage en cours',
    description: 'Le stage a démarré. Remplissez votre journal au fil des jours.',
    tone: 'info',
  },
  COMPLETED: {
    label: 'Stage terminé',
    description: 'Le stage est clôturé. Le journal est fermé et la note est définitive.',
    tone: 'ok',
  },
  ABANDONED: {
    label: 'Classée sans suite',
    description: 'Une autre de vos demandes a abouti, celle-ci n’a plus d’objet.',
    tone: 'neutral',
  },
};

/** Une demande close : elle ne figure plus parmi les demandes actives. */
export function estClose(statut: InternshipStatus): boolean {
  return (
    statut === 'REJECTED' ||
    statut === 'REFUSED' ||
    statut === 'COMPLETED' ||
    statut === 'ABANDONED'
  );
}

// --- Demandes de convention et de lettre d'affectation ---

export type RequestType =
  | 'DEMANDE_STAGE'
  | 'ATTESTATION_SCOLARITE'
  | 'CONVENTION'
  | 'LETTRE_AFFECTATION'
  | 'ATTESTATION_PRESENCE'
  /** Delivree par l'entreprise, pas par l'ecole. */
  | 'ATTESTATION_STAGE';
export type RequestStatus = 'PENDING' | 'ISSUED' | 'REJECTED';

export interface DocumentRequest {
  readonly id: number;
  readonly studentId: number;
  readonly studentName: string;
  readonly studentEmail: string | null;
  /** Nul pour une demande sans dossier : demande de stage, scolarité. */
  readonly internshipId: number | null;
  readonly internshipTitle: string | null;
  readonly companyName: string | null;
  readonly type: RequestType;
  readonly typeLabel: string;
  readonly status: RequestStatus;
  readonly reason: string | null;
  readonly processedBy: string | null;
  readonly processedAt: string | null;
  /** Identifiant du PDF rattaché une fois la demande éditée. */
  readonly documentId: number | null;
  readonly createdAt: string;
}

export const REQUEST_TYPE_LABELS: Readonly<Record<RequestType, string>> = {
  CONVENTION: 'Convention de stage',
  LETTRE_AFFECTATION: 'Lettre d’affectation',
  DEMANDE_STAGE: 'Demande de stage',
  ATTESTATION_SCOLARITE: 'Attestation de scolarité',
  ATTESTATION_PRESENCE: 'Attestation de présence',
  ATTESTATION_STAGE: 'Attestation de stage',
};

export const REQUEST_STATUS_LABELS: Readonly<Record<RequestStatus, string>> = {
  PENDING: 'En attente',
  ISSUED: 'Éditée',
  REJECTED: 'Refusée',
};
