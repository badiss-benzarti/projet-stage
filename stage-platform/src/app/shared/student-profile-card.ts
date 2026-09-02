import { ChangeDetectionStrategy, Component, effect, inject, input, signal } from '@angular/core';

import { StudentProfile, UserService } from '../core/services/user.service';
import { telechargerBlob } from './download';

/**
 * Fiche du stagiaire : coordonnees, cursus et CV.
 *
 * Le dossier de stage ne porte que le nom et le courriel de l'etudiant ;
 * tout le reste vit dans user-service. Ce bloc est partage par le service
 * des stages et l'entreprise, qui en ont besoin pour les memes raisons -
 * joindre le candidat et connaitre son cursus avant de se prononcer.
 *
 * Il charge lui-meme la fiche a partir de l'identifiant : les ecrans qui
 * l'affichent n'ont pas a s'en occuper. Un echec reste silencieux et
 * n'affiche rien, plutot que de faire echouer la page qui l'accueille.
 */
@Component({
  selector: 'gs-student-profile-card',
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    @if (profil(); as p) {
      <p class="eyebrow mb-2.5">Le stagiaire</p>
      <dl class="space-y-2 text-sm">
        <div class="flex justify-between gap-3">
          <dt class="text-sand-500">Courriel</dt>
          <dd class="truncate text-sand-800">{{ p.email }}</dd>
        </div>
        @if (p.phone) {
          <div class="flex justify-between gap-3">
            <dt class="text-sand-500">Téléphone</dt>
            <dd class="text-sand-800 tabular">{{ p.phone }}</dd>
          </div>
        }
        @if (p.cin) {
          <div class="flex justify-between gap-3">
            <dt class="text-sand-500">CIN</dt>
            <dd class="text-sand-800 tabular">{{ p.cin }}</dd>
          </div>
        }
        <div class="flex justify-between gap-3">
          <dt class="text-sand-500">Classe</dt>
          <dd class="text-sand-800">{{ p.classe }}</dd>
        </div>
        <div class="flex justify-between gap-3">
          <dt class="text-sand-500">Département</dt>
          <dd class="text-sand-800">{{ p.departement }}</dd>
        </div>
        @if (p.institutionName) {
          <div class="flex justify-between gap-3">
            <dt class="text-sand-500">Établissement</dt>
            <dd class="text-sand-800">{{ p.institutionName }}</dd>
          </div>
        }
        @if (p.academicLevel) {
          <div class="flex justify-between gap-3">
            <dt class="text-sand-500">Niveau</dt>
            <dd class="text-sand-800 tabular">{{ p.academicLevel }}<sup>e</sup> année</dd>
          </div>
        }
        @if (adresse(p); as ligne) {
          <div class="flex justify-between gap-3">
            <dt class="shrink-0 text-sand-500">Adresse</dt>
            <dd class="text-right text-sand-800">{{ ligne }}</dd>
          </div>
        }
      </dl>

      <div class="mt-3">
        @if (p.hasCv) {
          <button
            type="button"
            (click)="telechargerCv(p)"
            class="rounded-[5px] border border-sand-300 px-3 py-1.5 text-sm text-sand-700 transition-colors hover:border-petrol-400 hover:text-petrol-700"
          >
            Télécharger le CV
          </button>
        } @else {
          <p class="text-xs text-sand-500">Aucun CV déposé par l’étudiant.</p>
        }
      </div>
    }
  `,
})
export class StudentProfileCard {
  private readonly users = inject(UserService);

  readonly studentId = input.required<number>();

  protected readonly profil = signal<StudentProfile | null>(null);

  constructor() {
    effect(() => {
      const id = this.studentId();
      this.profil.set(null);
      this.users.studentById(id).subscribe({
        next: (p) => this.profil.set(p),
        error: () => this.profil.set(null),
      });
    });
  }

  /** Adresse en une ligne, sans virgules orphelines. */
  protected adresse(p: StudentProfile): string {
    return [p.address, p.city, p.governorateLabel].filter(Boolean).join(', ');
  }

  protected telechargerCv(p: StudentProfile): void {
    this.users.studentCvBlob(p.id).subscribe({
      next: (blob) => telechargerBlob(blob, p.cvName ?? `cv-${p.lastName}.pdf`),
    });
  }
}
