import { Role } from '../core/models/auth.models';

export interface NavItem {
  readonly label: string;
  readonly route: string;
  readonly exact?: boolean;
}

export interface NavSection {
  readonly title: string;
  readonly items: readonly NavItem[];
}

/**
 * Navigation par role.
 *
 * Ce n'est qu'un confort d'affichage : masquer un lien n'empeche personne
 * d'appeler l'API. Les autorisations reelles sont appliquees par les
 * gardes de route et, surtout, par le backend.
 */
export const NAV_BY_ROLE: Readonly<Record<Role, readonly NavSection[]>> = {
  ETUDIANT: [
    {
      title: 'Mon stage',
      items: [
        { label: 'Tableau de bord', route: '/etudiant', exact: true },
        { label: 'Mon profil', route: '/etudiant/profil' },
        { label: 'Ma demande', route: '/etudiant/demande' },
        { label: 'Journal de stage', route: '/etudiant/journal' },
        { label: 'Documents', route: '/etudiant/documents' },
      ],
    },
    {
      title: 'Évaluation',
      items: [
        { label: 'Ma note', route: '/etudiant/note' },
        { label: 'Réclamations', route: '/etudiant/reclamations' },
      ],
    },
  ],
  ENTREPRISE: [
    {
      title: 'Entreprise',
      items: [
        { label: 'Tableau de bord', route: '/entreprise', exact: true },
        { label: 'Profil de l’entreprise', route: '/entreprise/profil' },
        { label: 'Demandes reçues', route: '/entreprise/demandes' },
        { label: 'Mes stagiaires', route: '/entreprise/stagiaires' },
        { label: 'Encadrants', route: '/entreprise/encadrants' },
      ],
    },
  ],
  ENCADRANT: [
    {
      title: 'Encadrement',
      items: [
        { label: 'Tableau de bord', route: '/encadrant', exact: true },
        { label: 'Journaux à valider', route: '/encadrant/journaux' },
        { label: 'Grilles d’évaluation', route: '/encadrant/evaluations' },
      ],
    },
  ],
  CHEF_DEPARTEMENT_STAGE: [
    {
      title: 'Service des stages',
      items: [
        { label: 'Tableau de bord', route: '/departement-stages', exact: true },
        { label: 'Demandes', route: '/departement-stages/demandes' },
        { label: 'Documents à valider', route: '/departement-stages/documents' },
        { label: 'Conventions et lettres', route: '/departement-stages/requetes' },
      ],
    },
  ],
  CHEF_DEPARTEMENT_PEDAGOGIQUE: [
    {
      title: 'Département pédagogique',
      items: [
        { label: 'Tableau de bord', route: '/departement-pedagogique', exact: true },
        { label: 'Notes et évaluations', route: '/departement-pedagogique/notes' },
        { label: 'Réclamations', route: '/departement-pedagogique/reclamations' },
        { label: 'Suivi du risque', route: '/departement-pedagogique/risques' },
      ],
    },
  ],
  ADMIN: [
    {
      title: 'Administration',
      items: [
        { label: 'Tableau de bord', route: '/departement-stages', exact: true },
        { label: 'Demandes', route: '/departement-stages/demandes' },
      ],
    },
  ],
};
