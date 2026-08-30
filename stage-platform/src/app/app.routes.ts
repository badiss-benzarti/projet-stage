import { Routes } from '@angular/router';

import { authGuard, guestGuard, roleGuard } from './core/guards/auth.guard';


/**
 * Routage de l'application.
 *
 * Tout est charge a la demande : chaque espace n'est telecharge que
 * lorsqu'un utilisateur du role correspondant y accede.
 *
 * Les gardes de role ne font que masquer des pages : la vraie
 * autorisation est appliquee par le backend, qui renvoie 403 quoi
 * qu'affiche le frontend.
 */
export const routes: Routes = [
  {
    path: '',
    pathMatch: 'full',
    canActivate: [guestGuard],
    title: 'Plateforme de gestion des stages',
    loadComponent: () => import('./features/auth/welcome-page').then((m) => m.WelcomePage),
  },
  {
    path: 'connexion',
    title: 'Connexion — Gestion des stages',
    loadComponent: () => import('./features/auth/login-page').then((m) => m.LoginPage),
  },
  {
    path: 'inscription',
    pathMatch: 'full',
    canActivate: [guestGuard],
    title: 'Créer un compte — Gestion des stages',
    loadComponent: () => import('./features/auth/account-type-page').then((m) => m.AccountTypePage),
  },
  {
    path: 'inscription/:role',
    canActivate: [guestGuard],
    title: 'Créer un compte',
    loadComponent: () => import('./features/auth/register-page').then((m) => m.RegisterPage),
  },
  {
    path: 'acces-refuse',
    title: 'Accès refusé',
    loadComponent: () => import('./shared/access-denied').then((m) => m.AccessDenied),
  },
  {
    path: '',
    canActivate: [authGuard],
    loadComponent: () => import('./layout/shell').then((m) => m.Shell),
    children: [
      {
        path: 'etudiant',
        canActivate: [roleGuard('ETUDIANT')],
        children: [
          {
            path: '',
            title: 'Mon stage',
            loadComponent: () =>
              import('./features/student/student-dashboard').then((m) => m.StudentDashboard),
          },
          { path: 'profil', title: 'Mon profil',
            loadComponent: () => import('./features/student/profile-page').then((m) => m.ProfilePage) },
          { path: 'demande', title: 'Ma demande',
            loadComponent: () => import('./features/student/internship-request-page').then((m) => m.InternshipRequestPage) },
          { path: 'journal', title: 'Journal de stage',
            loadComponent: () => import('./features/student/journal-page').then((m) => m.JournalPage) },
          { path: 'documents', title: 'Mes documents', loadComponent: () => import('./features/student/documents-page').then((m) => m.DocumentsPage) },
          { path: 'note', title: 'Ma note', loadComponent: () => import('./features/student/grade-page').then((m) => m.GradePage) },
          { path: 'reclamations', title: 'Mes réclamations', loadComponent: () => import('./features/student/claims-page').then((m) => m.ClaimsPage) },
        ],
      },
      {
        path: 'entreprise',
        canActivate: [roleGuard('ENTREPRISE')],
        children: [
          { path: '', title: 'Entreprise', loadComponent: () => import('./features/company/company-dashboard').then((m) => m.CompanyDashboard) },
          { path: 'profil', title: 'Profil de l’entreprise',
            loadComponent: () => import('./features/company/company-profile-page').then((m) => m.CompanyProfilePage) },
          { path: 'demandes', title: 'Demandes reçues', loadComponent: () => import('./features/company/company-requests-page').then((m) => m.CompanyRequestsPage) },
          { path: 'stagiaires', title: 'Mes stagiaires', loadComponent: () => import('./features/company/company-interns-page').then((m) => m.CompanyInternsPage) },
          { path: 'encadrants', title: 'Encadrants', loadComponent: () => import('./features/company/company-supervisors-page').then((m) => m.CompanySupervisorsPage) },
        ],
      },
      {
        path: 'encadrant',
        canActivate: [roleGuard('ENCADRANT')],
        children: [
          { path: '', title: 'Encadrement', loadComponent: () => import('./features/supervisor/supervisor-dashboard').then((m) => m.SupervisorDashboard) },
          { path: 'journaux', title: 'Journaux à valider', loadComponent: () => import('./features/supervisor/supervisor-journal-page').then((m) => m.SupervisorJournalPage) },
          { path: 'evaluations', title: 'Grille d’évaluation', loadComponent: () => import('./features/supervisor/supervisor-evaluation-page').then((m) => m.SupervisorEvaluationPage) },
        ],
      },
      {
        path: 'departement-stages',
        canActivate: [roleGuard('CHEF_DEPARTEMENT_STAGE', 'ADMIN')],
        children: [
          { path: '', title: 'Service des stages', loadComponent: () => import('./features/department/stages-dashboard').then((m) => m.StagesDashboard) },
          { path: 'demandes', title: 'Demandes de stage', loadComponent: () => import('./features/department/internship-review-page').then((m) => m.InternshipReviewPage) },
          { path: 'documents', title: 'Documents à valider', loadComponent: () => import('./features/department/documents-review-page').then((m) => m.DocumentsReviewPage) },
          { path: 'requetes', title: 'Conventions et lettres', loadComponent: () => import('./features/department/requests-page').then((m) => m.RequestsPage) },
        ],
      },
      {
        path: 'departement-pedagogique',
        canActivate: [roleGuard('CHEF_DEPARTEMENT_PEDAGOGIQUE', 'ADMIN')],
        children: [
          { path: '', title: 'Département pédagogique', loadComponent: () => import('./features/department/pedagogy-dashboard').then((m) => m.PedagogyDashboard) },
          { path: 'notes', title: 'Notes et évaluations', loadComponent: () => import('./features/department/grades-page').then((m) => m.GradesPage) },
          { path: 'reclamations', title: 'Réclamations', loadComponent: () => import('./features/department/department-claims-page').then((m) => m.DepartmentClaimsPage) },
          { path: 'risques', title: 'Suivi du risque', loadComponent: () => import('./features/department/risk-page').then((m) => m.RiskPage) },
        ],
      },
      {
        path: 'notifications',
        title: 'Notifications',
        loadComponent: () =>
          import('./features/notifications/notifications-page').then((m) => m.NotificationsPage),
      },
    ],
  },
  { path: '**', redirectTo: '' },
];
