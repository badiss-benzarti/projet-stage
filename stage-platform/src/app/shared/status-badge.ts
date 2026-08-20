import { ChangeDetectionStrategy, Component, computed, input } from '@angular/core';

import { InternshipStatus, STATUS_META } from '../core/models/internship.models';

/**
 * Pastille d'etat du workflow.
 *
 * La couleur porte l'information, elle ne decore pas : un etat refuse est
 * rouge, un etat en attente est ambre. Un point plein double le signal
 * pour rester lisible en cas de daltonisme.
 */
@Component({
  selector: 'gs-status-badge',
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <span
      class="inline-flex items-center gap-1.5 rounded-full border px-2.5 py-0.5 text-xs font-medium whitespace-nowrap"
      [class]="classes()"
    >
      <span class="size-1.5 rounded-full" [class]="dot()"></span>
      {{ meta().label }}
    </span>
  `,
})
export class StatusBadge {
  readonly status = input.required<InternshipStatus>();

  protected readonly meta = computed(() => STATUS_META[this.status()]);

  protected readonly classes = computed(() => {
    switch (this.meta().tone) {
      case 'ok':
        return 'border-ok-500/25 bg-ok-50 text-ok-700';
      case 'warn':
        return 'border-warn-500/25 bg-warn-50 text-warn-700';
      case 'bad':
        return 'border-bad-500/25 bg-bad-50 text-bad-700';
      case 'info':
        return 'border-petrol-300/50 bg-petrol-50 text-petrol-700';
      default:
        return 'border-sand-300 bg-sand-100 text-sand-600';
    }
  });

  protected readonly dot = computed(() => {
    switch (this.meta().tone) {
      case 'ok':
        return 'bg-ok-500';
      case 'warn':
        return 'bg-warn-500';
      case 'bad':
        return 'bg-bad-500';
      case 'info':
        return 'bg-petrol-500';
      default:
        return 'bg-sand-400';
    }
  });
}
