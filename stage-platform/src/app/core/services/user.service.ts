import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { Page } from '../models/internship.models';

export interface Company {
  readonly id: number;
  readonly userId: number;
  readonly name: string;
  readonly address: string;
  readonly phone: string;
  readonly email: string;
  readonly taxId: string | null;
  readonly supervisorCount: number;
}

export interface StudentProfile {
  readonly id: number;
  readonly userId: number;
  readonly firstName: string;
  readonly lastName: string;
  readonly email: string;
  readonly phone: string | null;
  readonly cin: string | null;
  readonly classe: string;
  readonly departement: string;
}

export interface Supervisor {
  readonly id: number;
  readonly userId: number;
  readonly firstName: string;
  readonly lastName: string;
  readonly email: string;
  readonly phone: string | null;
  readonly position: string | null;
  readonly companyId: number;
  readonly companyName: string;
}

/** Profils : etudiants, entreprises, encadrants. */
@Injectable({ providedIn: 'root' })
export class UserService {
  private readonly http = inject(HttpClient);
  private readonly base = '/api/users';

  myStudentProfile(): Observable<StudentProfile> {
    return this.http.get<StudentProfile>(`${this.base}/students/me`);
  }

  saveMyStudentProfile(profil: Omit<StudentProfile, 'id' | 'userId'>): Observable<StudentProfile> {
    return this.http.post<StudentProfile>(`${this.base}/students/me`, profil);
  }

  updateMyStudentProfile(profil: Omit<StudentProfile, 'id' | 'userId'>): Observable<StudentProfile> {
    return this.http.put<StudentProfile>(`${this.base}/students/me`, profil);
  }

  companies(): Observable<Page<Company>> {
    return this.http.get<Page<Company>>(`${this.base}/companies`, {
      params: new HttpParams().set('size', 100),
    });
  }

  myCompany(): Observable<Company> {
    return this.http.get<Company>(`${this.base}/companies/me`);
  }

  supervisorsOf(companyId: number): Observable<readonly Supervisor[]> {
    return this.http.get<readonly Supervisor[]>(`${this.base}/companies/${companyId}/supervisors`);
  }

  mySupervisorProfile(): Observable<Supervisor> {
    return this.http.get<Supervisor>(`${this.base}/supervisors/me`);
  }
}
