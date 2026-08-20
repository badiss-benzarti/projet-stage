import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import {
  Claim,
  ClaimType,
  Evaluation,
  EvaluationRequest,
  RiskAssessment,
  Task,
  TaskRequest,
  TaskStatus,
  TaskSummary,
} from '../models/evaluation.models';
import { Page } from '../models/internship.models';

/** Module 2 : journal de stage, grille d'evaluation, reclamations, exports. */
@Injectable({ providedIn: 'root' })
export class EvaluationService {
  private readonly http = inject(HttpClient);
  private readonly base = '/api/evaluations';

  // ---- Journal ----

  tasks(internshipId: number, status?: TaskStatus): Observable<Page<Task>> {
    let params = new HttpParams().set('size', 100);
    if (status) {
      params = params.set('status', status);
    }
    return this.http.get<Page<Task>>(`${this.base}/internships/${internshipId}/tasks`, { params });
  }

  taskSummary(internshipId: number): Observable<TaskSummary> {
    return this.http.get<TaskSummary>(`${this.base}/internships/${internshipId}/tasks/summary`);
  }

  addTask(internshipId: number, task: TaskRequest): Observable<Task> {
    return this.http.post<Task>(`${this.base}/internships/${internshipId}/tasks`, task);
  }

  updateTask(taskId: number, task: TaskRequest): Observable<Task> {
    return this.http.put<Task>(`${this.base}/tasks/${taskId}`, task);
  }

  deleteTask(taskId: number): Observable<void> {
    return this.http.delete<void>(`${this.base}/tasks/${taskId}`);
  }

  decideTask(taskId: number, status: TaskStatus, reason?: string): Observable<Task> {
    return this.http.patch<Task>(`${this.base}/tasks/${taskId}/decision`, { status, reason });
  }

  // ---- Grille et note ----

  evaluation(internshipId: number): Observable<Evaluation> {
    return this.http.get<Evaluation>(`${this.base}/internships/${internshipId}`);
  }

  saveEvaluation(internshipId: number, grille: EvaluationRequest): Observable<Evaluation> {
    return this.http.put<Evaluation>(`${this.base}/internships/${internshipId}`, grille);
  }

  submitEvaluation(internshipId: number): Observable<Evaluation> {
    return this.http.post<Evaluation>(`${this.base}/internships/${internshipId}/submit`, {});
  }

  statistics(): Observable<Record<string, number>> {
    return this.http.get<Record<string, number>>(`${this.base}/statistics`);
  }

  risk(internshipId: number): Observable<RiskAssessment> {
    return this.http.get<RiskAssessment>(`${this.base}/internships/${internshipId}/risk`);
  }

  // ---- Reclamations ----

  openClaim(
    internshipId: number,
    type: ClaimType,
    subject: string,
    message: string,
  ): Observable<Claim> {
    return this.http.post<Claim>(`${this.base}/claims`, { internshipId, type, subject, message });
  }

  claim(id: number): Observable<Claim> {
    return this.http.get<Claim>(`${this.base}/claims/${id}`);
  }

  myClaims(): Observable<Page<Claim>> {
    return this.http.get<Page<Claim>>(`${this.base}/claims/mine`, {
      params: new HttpParams().set('size', 50),
    });
  }

  departmentClaims(): Observable<Page<Claim>> {
    return this.http.get<Page<Claim>>(`${this.base}/claims`, {
      params: new HttpParams().set('size', 50),
    });
  }

  replyToClaim(id: number, content: string): Observable<Claim> {
    return this.http.post<Claim>(`${this.base}/claims/${id}/messages`, { content });
  }

  takeClaim(id: number): Observable<Claim> {
    return this.http.post<Claim>(`${this.base}/claims/${id}/take`, {});
  }

  closeClaim(id: number, content: string): Observable<Claim> {
    return this.http.post<Claim>(`${this.base}/claims/${id}/close`, { content });
  }

  // ---- Exports : le navigateur telecharge le flux binaire ----

  journalPdf(internshipId: number): Observable<Blob> {
    return this.http.get(`${this.base}/internships/${internshipId}/journal/pdf`, {
      responseType: 'blob',
    });
  }

  notesXlsx(): Observable<Blob> {
    return this.http.get(`${this.base}/export/xlsx`, { responseType: 'blob' });
  }
}
