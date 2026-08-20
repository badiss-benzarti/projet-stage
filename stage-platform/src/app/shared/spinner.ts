import { ChangeDetectionStrategy, Component, input } from '@angular/core';

@Component({
  selector: 'gs-spinner',
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <div class="flex items-center justify-center gap-2.5 py-10 text-sm text-sand-500">
      <span
        class="size-3.5 animate-spin rounded-full border-2 border-sand-300 border-t-petrol-600"
        aria-hidden="true"
      ></span>
      {{ label() }}
    </div>
  `,
})
export class Spinner {
  readonly label = input<string>('Chargement…');
}
