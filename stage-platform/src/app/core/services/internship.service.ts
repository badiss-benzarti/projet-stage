import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import {
  Internship,
  InternshipRequest,
  InternshipStatus,
  Page,
  TransitionRequest,
  RequestStatus,
  RequestType,
  DocumentRequest,
} from '../models/internship.models';

/** Module 1 : demandes de stage et workflow de validation. */
@Injectable({ providedIn: 'root' })
export class InternshipService {
  private readonly http = inject(HttpClient);
  private readonly base = '/api/internships';

  /**
   * Mes dossiers, le plus recent d'abord.
   *
   * Sans tri explicite, la base renvoie l'ordre d'insertion : le tableau
   * de bord afficherait le premier stage jamais depose plutot que celui
   * en cours.
   */
  mine(page = 0, size = 10): Observable<Page<Internship>> {
    return this.http.get<Page<Internship>>(`${this.base}/mine`, {
      params: new HttpParams().set('page', page).set('size', size).set('sort', 'id,desc'),
    });
  }

  forCompany(status?: InternshipStatus, page = 0, size = 20): Observable<Page<Internship>> {
    let params = new HttpParams().set('page', page).set('size', size);
    if (status) {
      params = params.set('status', status);
    }
    return this.http.get<Page<Internship>>(`${this.base}/company`, { params });
  }

  forSupervision(page = 0, size = 20): Observable<Page<Internship>> {
    return this.http.get<Page<Internship>>(`${this.base}/supervision`, {
      params: new HttpParams().set('page', page).set('size', size),
    });
  }

  forDepartment(status?: InternshipStatus, page = 0, size = 20): Observable<Page<Internship>> {
    let params = new HttpParams().set('page', page).set('size', size);
    if (status) {
      params = params.set('status', status);
    }
    return this.http.get<Page<Internship>>(`${this.base}/department`, { params });
  }

  byId(id: number): Observable<Internship> {
    return this.http.get<Internship>(`${this.base}/${id}`);
  }

  create(demande: InternshipRequest): Observable<Internship> {
    return this.http.post<Internship>(this.base, demande);
  }

  updateDraft(id: number, demande: InternshipRequest): Observable<Internship> {
    return this.http.put<Internship>(`${this.base}/${id}`, demande);
  }

  /**
   * Point d'entree unique du workflow : le backend valide la transition,
   * le role et l'obligation de motif. Le frontend ne rejoue pas la regle,
   * il se contente d'afficher les availableActions renvoyees.
   */
  transition(id: number, requete: TransitionRequest): Observable<Internship> {
    return this.http.post<Internship>(`${this.base}/${id}/transition`, requete);
  }
  // ---- Demandes de convention et de lettre d'affectation ----

  pendingRequests(): Observable<Page<DocumentRequest>> {
    return this.http.get<Page<DocumentRequest>>(`${this.base}/requests/pending`, {
      params: new HttpParams().set('size', 50),
    });
  }

  requestsOf(internshipId: number): Observable<readonly DocumentRequest[]> {
    return this.http.get<readonly DocumentRequest[]>(`${this.base}/${internshipId}/requests`);
  }

  askDocument(internshipId: number, type: RequestType): Observable<DocumentRequest> {
    return this.http.post<DocumentRequest>(`${this.base}/${internshipId}/requests`, { type });
  }

  decideRequest(
    requestId: number,
    status: RequestStatus,
    reason?: string,
  ): Observable<DocumentRequest> {
    return this.http.patch<DocumentRequest>(`${this.base}/requests/${requestId}`, {
      status,
      reason,
    });
  }

  statistics(): Observable<Record<InternshipStatus, number>> {
    return this.http.get<Record<InternshipStatus, number>>(`${this.base}/statistics`);
  }
}
