import { ChangeDetectionStrategy, Component, inject } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { toSignal } from '@angular/core/rxjs-interop';
import { map } from 'rxjs';

/**
 * Page en attente d'implementation.
 *
 * Presente dans le routage pour que la navigation soit complete et
 * navigable des maintenant, plutot que de laisser des liens morts.
 */
@Component({
  selector: 'gs-placeholder-page',
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <div class="rounded-[6px] border border-dashed border-sand-300 bg-white px-6 py-16 text-center">
      <p class="eyebrow">{{ titre() }}</p>
      <p class="mt-2 text-sm text-sand-500">Écran en cours de développement.</p>
    </div>
  `,
})
export class PlaceholderPage {
  private readonly route = inject(ActivatedRoute);

  protected readonly titre = toSignal(
    this.route.data.pipe(map((d) => (d['titre'] as string | undefined) ?? 'Page')),
    { initialValue: 'Page' },
  );
}
