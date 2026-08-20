/** Les six roles de la plateforme, alignes sur l'enum du backend. */
export type Role =
  | 'ETUDIANT'
  | 'ENTREPRISE'
  | 'ENCADRANT'
  | 'CHEF_DEPARTEMENT_STAGE'
  | 'CHEF_DEPARTEMENT_PEDAGOGIQUE'
  | 'ADMIN';

export interface AuthUser {
  readonly id: number;
  readonly email: string;
  readonly firstName: string;
  readonly lastName: string;
  readonly role: Role;
  readonly enabled: boolean;
}

export interface LoginRequest {
  readonly email: string;
  readonly password: string;
}

export interface RegisterRequest extends LoginRequest {
  readonly firstName: string;
  readonly lastName: string;
  readonly role: Role;
}

export interface AuthResponse {
  readonly token: string;
  readonly tokenType: string;
  readonly expiresIn: number;
  readonly user: AuthUser;
}

/** Libelles affiches. Le backend ne renvoie que la valeur technique. */
export const ROLE_LABELS: Readonly<Record<Role, string>> = {
  ETUDIANT: 'Étudiant',
  ENTREPRISE: 'Entreprise',
  ENCADRANT: 'Encadrant',
  CHEF_DEPARTEMENT_STAGE: 'Chef de département — Stages',
  CHEF_DEPARTEMENT_PEDAGOGIQUE: 'Chef de département — Pédagogique',
  ADMIN: 'Administrateur',
};

/** Espace d'accueil de chaque role apres connexion. */
export const ROLE_HOME: Readonly<Record<Role, string>> = {
  ETUDIANT: '/etudiant',
  ENTREPRISE: '/entreprise',
  ENCADRANT: '/encadrant',
  CHEF_DEPARTEMENT_STAGE: '/departement-stages',
  CHEF_DEPARTEMENT_PEDAGOGIQUE: '/departement-pedagogique',
  ADMIN: '/departement-stages',
};
