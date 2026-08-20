import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { Page } from '../models/internship.models';

export type DocumentType = 'CONVENTION' | 'LETTRE_AFFECTATION' | 'RAPPORT' | 'ATTESTATION';
export type DocumentStatus = 'UPLOADED' | 'UNDER_REVIEW' | 'APPROVED' | 'REJECTED';

export interface StageDocument {
  readonly id: number;
  readonly internshipId: number;
  readonly studentName: string | null;
  readonly type: DocumentType;
  readonly originalName: string;
  readonly contentType: string | null;
  readonly sizeBytes: number | null;
  readonly status: DocumentStatus;
  readonly rejectionReason: string | null;
  readonly generated: boolean;
  readonly uploadedBy: string | null;
  readonly validatedBy: string | null;
  readonly createdAt: string;
}

export const DOCUMENT_TYPE_LABELS: Readonly<Record<DocumentType, string>> = {
  CONVENTION: 'Convention de stage',
  LETTRE_AFFECTATION: 'Lettre d’affectation',
  RAPPORT: 'Rapport de stage',
  ATTESTATION: 'Attestation de stage',
};

export const DOCUMENT_STATUS_META: Readonly<
  Record<DocumentStatus, { label: string; tone: 'ok' | 'warn' | 'bad' }>
> = {
  UPLOADED: { label: 'Déposé', tone: 'warn' },
  UNDER_REVIEW: { label: 'En cours d’examen', tone: 'warn' },
  APPROVED: { label: 'Validé', tone: 'ok' },
  REJECTED: { label: 'Refusé', tone: 'bad' },
};

@Injectable({ providedIn: 'root' })
export class DocumentService {
  private readonly http = inject(HttpClient);
  private readonly base = '/api/documents';

  forInternship(internshipId: number): Observable<readonly StageDocument[]> {
    return this.http.get<readonly StageDocument[]>(`${this.base}/internships/${internshipId}`);
  }

  pending(): Observable<Page<StageDocument>> {
    return this.http.get<Page<StageDocument>>(`${this.base}/pending`, {
      params: new HttpParams().set('size', 50),
    });
  }

  upload(internshipId: number, type: DocumentType, fichier: File): Observable<StageDocument> {
    const corps = new FormData();
    corps.append('file', fichier);
    return this.http.post<StageDocument>(
      `${this.base}/internships/${internshipId}`,
      corps,
      { params: new HttpParams().set('type', type) },
    );
  }

  decide(id: number, status: DocumentStatus, reason?: string): Observable<StageDocument> {
    return this.http.patch<StageDocument>(`${this.base}/${id}/decision`, { status, reason });
  }

  remove(id: number): Observable<void> {
    return this.http.delete<void>(`${this.base}/${id}`);
  }

  generateAttestation(internshipId: number): Observable<StageDocument> {
    return this.http.post<StageDocument>(
      `${this.base}/internships/${internshipId}/attestation`,
      {},
    );
  }

  download(id: number): Observable<Blob> {
    return this.http.get(`${this.base}/${id}/download`, { responseType: 'blob' });
  }
}
