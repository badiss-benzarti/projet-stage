import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { toSignal } from '@angular/core/rxjs-interop';

import { Evaluation } from '../../core/models/evaluation.models';
import { Internship } from '../../core/models/internship.models';
import { EvaluationService } from '../../core/services/evaluation.service';
import { InternshipService } from '../../core/services/internship.service';
import { EmptyState } from '../../shared/empty-state';
import { Spinner } from '../../shared/spinner';

interface Critere {
  readonly cle: 'technicalScore' | 'qualityScore' | 'autonomyScore' | 'communicationScore' | 'punctualityScore';
  readonly libelle: string;
  readonly poids: number;
  readonly aide: string;
}

/** Le bareme, aligne sur config-repo/evaluation-service.yml. */
const CRITERES: readonly Critere[] = [
  { cle: 'technicalScore', libelle: 'Compétences techniques', poids: 30,
    aide: 'Maîtrise des outils et méthodes du poste' },
  { cle: 'qualityScore', libelle: 'Qualité du travail rendu', poids: 20,
    aide: 'Fiabilité, finition, respect des consignes' },
  { cle: 'autonomyScore', libelle: 'Autonomie et initiative', poids: 20,
    aide: 'Capacité à avancer sans supervision continue' },
  { cle: 'communicationScore', libelle: 'Communication et intégration', poids: 15,
    aide: 'Insertion dans l’équipe, restitution' },
  { cle: 'punctualityScore', libelle: 'Assiduité et ponctualité', poids: 15,
    aide: 'Présence, respect des horaires et délais' },
];

@Component({
  selector: 'gs-supervisor-evaluation-page',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [ReactiveFormsModule, EmptyState, Spinner],
  templateUrl: './supervisor-evaluation-page.html',
})
export class SupervisorEvaluationPage {
  private readonly fb = inject(FormBuilder);
  private readonly internships = inject(InternshipService);
  private readonly evaluations = inject(EvaluationService);

  protected readonly criteres = CRITERES;

  protected readonly chargement = signal(true);
  protected readonly envoi = signal(false);
  protected readonly erreur = signal<string | null>(null);
  protected readonly succes = signal<string | null>(null);

  protected readonly dossiers = signal<readonly Internship[]>([]);
  protected readonly selection = signal<Internship | null>(null);
  protected readonly grille = signal<Evaluation | null>(null);

  protected readonly figee = computed(() => this.grille()?.status === 'SUBMITTED');

  protected readonly form = this.fb.nonNullable.group({
    technicalScore: [14, [Validators.required, Validators.min(0), Validators.max(20)]],
    qualityScore: [14, [Validators.required, Validators.min(0), Validators.max(20)]],
    autonomyScore: [14, [Validators.required, Validators.min(0), Validators.max(20)]],
    communicationScore: [14, [Validators.required, Validators.min(0), Validators.max(20)]],
    punctualityScore: [14, [Validators.required, Validators.min(0), Validators.max(20)]],
    globalComment: ['', Validators.required],
    remarks: [''],
  });

  /** Valeurs du formulaire suivies en signal, pour l'apercu de la note. */
  private readonly valeurs = toSignal(this.form.valueChanges, {
    initialValue: this.form.getRawValue(),
  });

  /**
   * Apercu de la note, calcule cote client uniquement pour l'affichage.
   * La note qui fait foi est TOUJOURS celle que renvoie le backend :
   * ScoringService la recalcule a chaque enregistrement.
   */
  protected readonly apercu = computed(() => {
    const v = this.valeurs();
    const pondere = CRITERES.reduce((somme, c) => {
      const note = Number(v[c.cle] ?? 0);
      return somme + note * c.poids;
    }, 0);
    return Math.round(pondere / 100 / 0.25) * 0.25;
  });

  constructor() {
    this.internships.forSupervision().subscribe({
      next: (page) => {
        const eligibles = page.content.filter(
          (i) => i.status === 'IN_PROGRESS' || i.status === 'COMPLETED',
        );
        this.dossiers.set(eligibles);
        this.chargement.set(false);
        if (eligibles.length > 0) {
          this.choisir(eligibles[0]);
        }
      },
      error: () => this.chargement.set(false),
    });
  }

  protected choisir(dossier: Internship): void {
    this.selection.set(dossier);
    this.grille.set(null);
    this.erreur.set(null);
    this.succes.set(null);
    this.form.enable();

    this.evaluations.evaluation(dossier.id).subscribe({
      next: (e) => {
        this.grille.set(e);
        this.form.patchValue({
          technicalScore: e.technicalScore ?? 14,
          qualityScore: e.qualityScore ?? 14,
          autonomyScore: e.autonomyScore ?? 14,
          communicationScore: e.communicationScore ?? 14,
          punctualityScore: e.punctualityScore ?? 14,
          globalComment: e.globalComment ?? '',
          remarks: e.remarks ?? '',
        });
        if (e.status === 'SUBMITTED') {
          this.form.disable();
        }
      },
      // 404 : aucune grille encore, on part du formulaire vierge.
      error: () => this.grille.set(null),
    });
  }

  protected enregistrer(valider: boolean): void {
    const dossier = this.selection();
    if (!dossier || this.envoi()) {
      return;
    }
    if (valider && this.form.invalid) {
      this.form.markAllAsTouched();
      this.erreur.set('Renseignez les cinq critères et l’appréciation globale.');
      return;
    }

    this.envoi.set(true);
    this.erreur.set(null);
    this.succes.set(null);

    const v = this.form.getRawValue();
    this.evaluations
      .saveEvaluation(dossier.id, {
        technicalScore: Number(v.technicalScore),
        qualityScore: Number(v.qualityScore),
        autonomyScore: Number(v.autonomyScore),
        communicationScore: Number(v.communicationScore),
        punctualityScore: Number(v.punctualityScore),
        globalComment: v.globalComment.trim() || null,
        remarks: v.remarks.trim() || null,
      })
      .subscribe({
        next: (e) => {
          this.grille.set(e);
          if (!valider) {
            this.envoi.set(false);
            this.succes.set(`Grille enregistrée. Note provisoire : ${e.finalScore} / 20.`);
            return;
          }
          this.evaluations.submitEvaluation(dossier.id).subscribe({
            next: (finale) => {
              this.grille.set(finale);
              this.form.disable();
              this.envoi.set(false);
              this.succes.set(`Évaluation validée. Note définitive : ${finale.finalScore} / 20.`);
            },
            error: (err: { error?: { message?: string } }) => {
              this.envoi.set(false);
              this.erreur.set(err.error?.message ?? 'Validation impossible.');
            },
          });
        },
        error: (err: { error?: { message?: string } }) => {
          this.envoi.set(false);
          this.erreur.set(err.error?.message ?? 'Enregistrement impossible.');
        },
      });
  }
}
