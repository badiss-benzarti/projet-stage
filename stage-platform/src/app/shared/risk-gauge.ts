import { ChangeDetectionStrategy, Component, computed, input } from '@angular/core';

import { RiskAssessment } from '../core/models/evaluation.models';

/**
 * Jauge de risque issue du modele d'apprentissage.
 *
 * Le niveau seul est inexploitable : on affiche toujours les indicateurs
 * qui l'expliquent, pour que le responsable sache sur quoi agir.
 */
@Component({
  selector: 'gs-risk-gauge',
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <div class="rounded-[6px] border border-sand-200 bg-white px-5 py-4">
      <div class="flex items-baseline justify-between gap-3">
        <p class="eyebrow">Risque de difficulté</p>
        <span class="rounded-full border px-2.5 py-0.5 text-xs font-medium" [class]="pastille()">
          {{ libelle() }}
        </span>
      </div>

      @if (assessment().risk !== 'UNAVAILABLE') {
        <div class="mt-3 h-1.5 overflow-hidden rounded-full bg-sand-200">
          <div class="h-full rounded-full transition-all" [class]="barre()" [style.width.%]="pourcentage()"></div>
        </div>
        <p class="mt-1.5 text-xs text-sand-500 tabular">
          Confiance du modèle : {{ pourcentage() }} %
        </p>

        <ul class="mt-3 space-y-1">
          @for (raison of assessment().drivers; track raison) {
            <li class="flex gap-2 text-sm text-sand-600">
              <span class="mt-1.5 size-1 shrink-0 rounded-full bg-sand-400"></span>
              {{ raison }}
            </li>
          }
        </ul>
      } @else {
        <p class="mt-2 text-sm text-sand-500">
          Le service de prédiction est momentanément indisponible.
        </p>
      }
    </div>
  `,
})
export class RiskGauge {
  readonly assessment = input.required<RiskAssessment>();

  protected readonly pourcentage = computed(() =>
    Math.round((this.assessment().probability ?? 0) * 100),
  );

  protected readonly libelle = computed(() => {
    switch (this.assessment().risk) {
      case 'LOW':
        return 'Faible';
      case 'MEDIUM':
        return 'Modéré';
      case 'HIGH':
        return 'Élevé';
      default:
        return 'Indisponible';
    }
  });

  protected readonly pastille = computed(() => {
    switch (this.assessment().risk) {
      case 'LOW':
        return 'border-ok-500/25 bg-ok-50 text-ok-700';
      case 'MEDIUM':
        return 'border-warn-500/25 bg-warn-50 text-warn-700';
      case 'HIGH':
        return 'border-bad-500/25 bg-bad-50 text-bad-700';
      default:
        return 'border-sand-300 bg-sand-100 text-sand-500';
    }
  });

  protected readonly barre = computed(() => {
    switch (this.assessment().risk) {
      case 'LOW':
        return 'bg-ok-500';
      case 'MEDIUM':
        return 'bg-warn-500';
      default:
        return 'bg-bad-500';
    }
  });
}
