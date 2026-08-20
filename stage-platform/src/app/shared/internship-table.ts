import { ChangeDetectionStrategy, Component, input, output } from '@angular/core';

import { Internship } from '../core/models/internship.models';
import { EmptyState } from './empty-state';
import { StatusBadge } from './status-badge';

/**
 * Tableau de dossiers, partage par les espaces entreprise, encadrant et
 * departement. Chaque espace filtre ses donnees, la presentation est la
 * meme partout.
 */
@Component({
  selector: 'gs-internship-table',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [StatusBadge, EmptyState],
  template: `
    @if (internships().length === 0) {
      <gs-empty-state [title]="emptyTitle()" [hint]="emptyHint()" />
    } @else {
      <div class="overflow-x-auto">
        <table class="w-full text-sm">
          <thead>
            <tr class="border-b border-sand-200 text-left">
              <th class="eyebrow px-6 py-2.5 font-medium">Étudiant</th>
              <th class="eyebrow px-6 py-2.5 font-medium">Sujet</th>
              <th class="eyebrow px-6 py-2.5 font-medium">Type</th>
              @if (showCompany()) {
                <th class="eyebrow px-6 py-2.5 font-medium">Entreprise</th>
              }
              <th class="eyebrow px-6 py-2.5 font-medium">État</th>
              <th class="px-6 py-2.5"></th>
            </tr>
          </thead>
          <tbody class="divide-y divide-sand-100">
            @for (item of internships(); track item.id) {
              <tr class="transition-colors hover:bg-sand-50">
                <td class="px-6 py-3 font-medium whitespace-nowrap text-sand-900">
                  {{ item.studentName }}
                </td>
                <td class="max-w-xs truncate px-6 py-3 text-sand-600">{{ item.title }}</td>
                <td class="px-6 py-3 whitespace-nowrap text-sand-500">
                  {{ item.type === 'PFE' ? 'PFE' : 'Été' }}
                </td>
                @if (showCompany()) {
                  <td class="px-6 py-3 whitespace-nowrap text-sand-600">
                    {{ item.companyName ?? '—' }}
                  </td>
                }
                <td class="px-6 py-3"><gs-status-badge [status]="item.status" /></td>
                <td class="px-6 py-3 text-right whitespace-nowrap">
                  <button
                    type="button"
                    (click)="select.emit(item)"
                    class="text-sm font-medium text-petrol-700 hover:underline"
                  >
                    {{ actionLabel() }}
                  </button>
                </td>
              </tr>
            }
          </tbody>
        </table>
      </div>
    }
  `,
})
export class InternshipTable {
  readonly internships = input.required<readonly Internship[]>();
  readonly emptyTitle = input<string>('Aucun dossier');
  readonly emptyHint = input<string>();
  readonly actionLabel = input<string>('Ouvrir');
  readonly showCompany = input<boolean>(true);

  readonly select = output<Internship>();
}
