import { Routes } from '@angular/router';

import { authGuard, roleGuard } from './core/guards/auth.guard';

const placeholder = () => import('./shared/placeholder-page').then((m) => m.PlaceholderPage);

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
    path: 'connexion',
    title: 'Connexion — Gestion des stages',
    loadComponent: () => import('./features/auth/login-page').then((m) => m.LoginPage),
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
          { path: 'demande', title: 'Ma demande',
            loadComponent: () => import('./features/student/internship-request-page').then((m) => m.InternshipRequestPage) },
          { path: 'journal', title: 'Journal de stage',
            loadComponent: () => import('./features/student/journal-page').then((m) => m.JournalPage) },
          { path: 'documents', title: 'Documents', data: { titre: 'Mes documents' }, loadComponent: placeholder },
          { path: 'note', title: 'Ma note', data: { titre: 'Ma note de stage' }, loadComponent: placeholder },
          { path: 'reclamations', title: 'Réclamations', data: { titre: 'Mes réclamations' }, loadComponent: placeholder },
        ],
      },
      {
        path: 'entreprise',
        canActivate: [roleGuard('ENTREPRISE')],
        children: [
          { path: '', title: 'Entreprise', data: { titre: 'Tableau de bord entreprise' }, loadComponent: placeholder },
          { path: 'demandes', title: 'Demandes reçues', data: { titre: 'Demandes reçues' }, loadComponent: placeholder },
          { path: 'stagiaires', title: 'Mes stagiaires', data: { titre: 'Mes stagiaires' }, loadComponent: placeholder },
          { path: 'encadrants', title: 'Encadrants', data: { titre: 'Mes encadrants' }, loadComponent: placeholder },
        ],
      },
      {
        path: 'encadrant',
        canActivate: [roleGuard('ENCADRANT')],
        children: [
          { path: '', title: 'Encadrement', data: { titre: 'Tableau de bord encadrant' }, loadComponent: placeholder },
          { path: 'journaux', title: 'Journaux', data: { titre: 'Journaux à valider' }, loadComponent: placeholder },
          { path: 'evaluations', title: 'Évaluations', data: { titre: 'Grilles d’évaluation' }, loadComponent: placeholder },
        ],
      },
      {
        path: 'departement-stages',
        canActivate: [roleGuard('CHEF_DEPARTEMENT_STAGE', 'ADMIN')],
        children: [
          { path: '', title: 'Service des stages', data: { titre: 'Tableau de bord — service des stages' }, loadComponent: placeholder },
          { path: 'demandes', title: 'Demandes', data: { titre: 'Demandes de stage' }, loadComponent: placeholder },
          { path: 'documents', title: 'Documents', data: { titre: 'Documents à valider' }, loadComponent: placeholder },
          { path: 'requetes', title: 'Conventions', data: { titre: 'Conventions et lettres d’affectation' }, loadComponent: placeholder },
        ],
      },
      {
        path: 'departement-pedagogique',
        canActivate: [roleGuard('CHEF_DEPARTEMENT_PEDAGOGIQUE', 'ADMIN')],
        children: [
          { path: '', title: 'Département pédagogique', data: { titre: 'Tableau de bord — pédagogique' }, loadComponent: placeholder },
          { path: 'notes', title: 'Notes', data: { titre: 'Notes et évaluations' }, loadComponent: placeholder },
          { path: 'reclamations', title: 'Réclamations', data: { titre: 'Réclamations' }, loadComponent: placeholder },
          { path: 'risques', title: 'Risques', data: { titre: 'Suivi du risque' }, loadComponent: placeholder },
        ],
      },
      {
        path: 'notifications',
        title: 'Notifications',
        loadComponent: () =>
          import('./features/notifications/notifications-page').then((m) => m.NotificationsPage),
      },
      { path: '', pathMatch: 'full', redirectTo: 'etudiant' },
    ],
  },
  { path: '**', redirectTo: 'connexion' },
];
