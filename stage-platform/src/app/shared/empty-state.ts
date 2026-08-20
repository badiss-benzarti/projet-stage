import { ChangeDetectionStrategy, Component, input } from '@angular/core';

/** Etat vide : dire ce qui manque et quoi faire, jamais une page blanche. */
@Component({
  selector: 'gs-empty-state',
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <div class="flex flex-col items-center justify-center px-6 py-14 text-center">
      <div class="mb-3 flex size-10 items-center justify-center rounded-full bg-sand-200">
        <span class="text-lg text-sand-500">{{ icon() }}</span>
      </div>
      <p class="text-sm font-medium text-sand-700">{{ title() }}</p>
      @if (hint(); as h) {
        <p class="mt-1 max-w-sm text-sm text-sand-500">{{ h }}</p>
      }
      <ng-content />
    </div>
  `,
})
export class EmptyState {
  readonly title = input.required<string>();
  readonly hint = input<string>();
  readonly icon = input<string>('—');
}
