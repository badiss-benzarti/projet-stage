import { HttpClient } from '@angular/common/http';
import { Injectable, computed, inject, signal } from '@angular/core';
import { Router } from '@angular/router';
import { Observable, tap } from 'rxjs';

import {
  AuthResponse,
  AuthUser,
  LoginRequest,
  RegisterRequest,
  ROLE_HOME,
  Role,
} from '../models/auth.models';

const CLE_JETON = 'gs.token';
const CLE_UTILISATEUR = 'gs.user';

/**
 * Etat d'authentification de l'application.
 *
 * La source de verite est un signal : tout composant qui lit currentUser()
 * se remet a jour seul, sans souscription ni abonnement a nettoyer.
 * Le stockage local ne sert qu'a survivre a un rechargement de page.
 */
@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly http = inject(HttpClient);
  private readonly router = inject(Router);

  private readonly _token = signal<string | null>(this.lireJeton());
  private readonly _user = signal<AuthUser | null>(this.lireUtilisateur());

  readonly currentUser = this._user.asReadonly();
  readonly token = this._token.asReadonly();

  readonly isAuthenticated = computed(() => this._token() !== null);
  readonly role = computed<Role | null>(() => this._user()?.role ?? null);
  readonly fullName = computed(() => {
    const u = this._user();
    return u ? `${u.firstName} ${u.lastName}` : '';
  });
  readonly initials = computed(() => {
    const u = this._user();
    return u ? `${u.firstName.charAt(0)}${u.lastName.charAt(0)}`.toUpperCase() : '';
  });

  login(credentials: LoginRequest): Observable<AuthResponse> {
    return this.http
      .post<AuthResponse>('/api/auth/login', credentials)
      .pipe(tap((r) => this.enregistrerSession(r)));
  }

  register(demande: RegisterRequest): Observable<AuthResponse> {
    return this.http
      .post<AuthResponse>('/api/auth/register', demande)
      .pipe(tap((r) => this.enregistrerSession(r)));
  }

  logout(): void {
    localStorage.removeItem(CLE_JETON);
    localStorage.removeItem(CLE_UTILISATEUR);
    this._token.set(null);
    this._user.set(null);
    void this.router.navigate(['/connexion']);
  }

  /** Accueil correspondant au role, utilise apres connexion. */
  homeRoute(): string {
    const r = this.role();
    return r ? ROLE_HOME[r] : '/connexion';
  }

  hasAnyRole(...roles: readonly Role[]): boolean {
    const r = this.role();
    return r !== null && roles.includes(r);
  }

  private enregistrerSession(reponse: AuthResponse): void {
    localStorage.setItem(CLE_JETON, reponse.token);
    localStorage.setItem(CLE_UTILISATEUR, JSON.stringify(reponse.user));
    this._token.set(reponse.token);
    this._user.set(reponse.user);
  }

  private lireJeton(): string | null {
    return localStorage.getItem(CLE_JETON);
  }

  private lireUtilisateur(): AuthUser | null {
    const brut = localStorage.getItem(CLE_UTILISATEUR);
    if (!brut) {
      return null;
    }
    try {
      return JSON.parse(brut) as AuthUser;
    } catch {
      // Stockage corrompu : on repart d'une session vide plutot que de planter.
      localStorage.removeItem(CLE_UTILISATEUR);
      return null;
    }
  }
}
