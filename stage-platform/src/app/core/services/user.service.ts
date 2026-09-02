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
  /** Presentation libre de l'entreprise, affichée aux étudiants. */
  readonly description: string | null;
  readonly supervisorCount: number;
}

export type InstitutionType = 'PUBLIQUE' | 'PRIVEE';

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
  readonly institutionName: string | null;
  readonly institutionType: InstitutionType | null;
  readonly academicLevel: number | null;
  readonly address: string | null;
  readonly city: string | null;
  readonly governorate: string | null;
  readonly governorateLabel: string | null;
  readonly hasPhoto: boolean;
  readonly hasCv: boolean;
  /** Nom du fichier tel que l'etudiant l'a depose, null s'il n'y a pas de CV. */
  readonly cvName: string | null;
}

/** Ce que le formulaire envoie : le profil sans ses champs calcules. */
export type StudentProfileRequest = Omit<
  StudentProfile,
  'id' | 'userId' | 'governorateLabel' | 'hasPhoto' | 'hasCv' | 'cvName'
>;

export interface Option {
  readonly value: string;
  readonly label: string;
}

export interface Referentiels {
  readonly gouvernorats: readonly Option[];
  readonly typesEtablissement: readonly Option[];
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

/**
 * Encadrant tel que le voit un etudiant qui depose sa demande.
 *
 * Ni email ni telephone : le backend ne les sert pas a ce role, pour ne
 * pas faire de la liste des partenaires un annuaire moissonnable.
 */
export interface SupervisorOption {
  readonly id: number;
  readonly fullName: string;
  readonly position: string | null;
  readonly companyId: number;
}

/** Profils : etudiants, entreprises, encadrants. */
@Injectable({ providedIn: 'root' })
export class UserService {
  private readonly http = inject(HttpClient);
  private readonly base = '/api/users';

  myStudentProfile(): Observable<StudentProfile> {
    return this.http.get<StudentProfile>(`${this.base}/students/me`);
  }

  saveMyStudentProfile(profil: StudentProfileRequest): Observable<StudentProfile> {
    return this.http.post<StudentProfile>(`${this.base}/students/me`, profil);
  }

  updateMyStudentProfile(profil: StudentProfileRequest): Observable<StudentProfile> {
    return this.http.put<StudentProfile>(`${this.base}/students/me`, profil);
  }

  /** Photo de profil : multipart, remplace la precedente. */
  uploadMyPhoto(fichier: File): Observable<StudentProfile> {
    const corps = new FormData();
    corps.append('file', fichier);
    return this.http.post<StudentProfile>(`${this.base}/students/me/photo`, corps);
  }

  /**
   * Photo d'un etudiant, recuperee en blob.
   *
   * Une balise img ne porte pas l'en-tete Authorization : ouvrir
   * l'endpoint rendrait toutes les photos lisibles par simple increment
   * d'identifiant. On passe donc par HttpClient, puis par une URL objet.
   */
  photoBlob(studentId: number): Observable<Blob> {
    return this.http.get(`${this.base}/students/${studentId}/photo`, {
      responseType: 'blob',
    });
  }

  /** CV de l'etudiant : PDF, remplace le precedent. */
  uploadMyCv(fichier: File): Observable<StudentProfile> {
    const corps = new FormData();
    corps.append('file', fichier);
    return this.http.post<StudentProfile>(`${this.base}/students/me/cv`, corps);
  }

  deleteMyCv(): Observable<StudentProfile> {
    return this.http.delete<StudentProfile>(`${this.base}/students/me/cv`);
  }

  /** Son propre CV, en blob : le lien direct ne porte pas le jeton. */
  myCvBlob(): Observable<Blob> {
    return this.http.get(`${this.base}/students/me/cv`, { responseType: 'blob' });
  }

  /** Fiche complete d'un etudiant, pour l'encadrant et les departements. */
  studentById(studentId: number): Observable<StudentProfile> {
    return this.http.get<StudentProfile>(`${this.base}/students/${studentId}`);
  }

  /** Le CV d'un candidat, pour l'entreprise et les departements. */
  studentCvBlob(studentId: number): Observable<Blob> {
    return this.http.get(`${this.base}/students/${studentId}/cv`, { responseType: 'blob' });
  }

  /** Encadrants declares par une entreprise, proposables a un etudiant. */
  supervisorOptionsOf(companyId: number): Observable<readonly SupervisorOption[]> {
    return this.http.get<readonly SupervisorOption[]>(
      `${this.base}/companies/${companyId}/supervisors/options`,
    );
  }

  /** Gouvernorats et types d'etablissement, servis par le backend. */
  referentiels(): Observable<Referentiels> {
    return this.http.get<Referentiels>(`${this.base}/students/referentiels`);
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
