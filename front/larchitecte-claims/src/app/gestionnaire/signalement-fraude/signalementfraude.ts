import { Component, inject, signal, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { FraudAlertService, FraudAlertRequest, FraudAlertResponse } from '../../core/fraud-alert.service';
import { ClaimService, ClaimResponse } from '../../core/claim.service';

@Component({
  selector: 'app-signalement-fraude',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './signalementfraude.html'
})
export class SignalementFraude implements OnInit {
  private fraudAlertService = inject(FraudAlertService);
  private claimService = inject(ClaimService);

  claims = signal<ClaimResponse[]>([]);
  alerts = signal<FraudAlertResponse[]>([]);
  loading = signal(true);
  submitting = signal(false);
  error = signal('');
  success = signal('');

  // Formulaire de signalement
  showForm = signal(false);
  selectedClaim = signal<ClaimResponse | null>(null);
  motif = signal('');
  description = signal('');
  niveauRisque = signal('MOYEN');

  motifs = [
    { value: 'documents_falsifies', label: 'Documents falsifiés' },
    { value: 'incoherence_reclamations', label: 'Incohérence des réclamations' },
    { value: 'surevaluation_degats', label: 'Surévaluation des dégâts' },
    { value: 'sinistre_fictif', label: 'Sinistre fictif' },
    { value: 'recurrence_suspecte', label: 'Récurrence suspecte' },
    { value: 'autre', label: 'Autre' }
  ];

  niveauxRisque = [
    { value: 'FAIBLE', label: 'Faible', class: 'bg-green-100 text-green-800' },
    { value: 'MOYEN', label: 'Moyen', class: 'bg-amber-100 text-amber-800' },
    { value: 'ELEVE', label: 'Élevé', class: 'bg-orange-100 text-orange-800' },
    { value: 'CRITIQUE', label: 'Critique', class: 'bg-red-100 text-red-800' }
  ];

  ngOnInit(): void {
    this.loadData();
  }

  loadData(): void {
    this.loading.set(true);
    this.error.set('');

    this.claimService.getAssignedClaims().subscribe({
      next: (data) => {
        this.claims.set(data);
        this.loading.set(false);
      },
      error: () => {
        this.error.set('Erreur lors du chargement des dossiers');
        this.loading.set(false);
      }
    });

    this.fraudAlertService.getMyFraudAlerts().subscribe({
      next: (data) => this.alerts.set(data),
      error: () => {}
    });
  }

  openForm(claim: ClaimResponse): void {
    this.selectedClaim.set(claim);
    this.showForm.set(true);
    this.motif.set('');
    this.description.set('');
    this.niveauRisque.set('MOYEN');
    this.success.set('');
    this.error.set('');
  }

  closeForm(): void {
    this.showForm.set(false);
    this.selectedClaim.set(null);
  }

  submitSignalement(): void {
    const claim = this.selectedClaim();
    if (!claim || !this.motif()) return;

    this.submitting.set(true);
    this.error.set('');
    this.success.set('');

    const request: FraudAlertRequest = {
      claimId: claim.id,
      motif: this.motif(),
      description: this.description() || undefined,
      niveauRisque: this.niveauRisque()
    };

    this.fraudAlertService.createFraudAlert(request).subscribe({
      next: () => {
        this.submitting.set(false);
        this.success.set('Signalement de fraude soumis avec succès. Les administrateurs ont été notifiés.');
        this.showForm.set(false);
        this.selectedClaim.set(null);
        this.fraudAlertService.getMyFraudAlerts().subscribe({
          next: (data) => this.alerts.set(data),
          error: () => {}
        });
      },
      error: () => {
        this.error.set('Erreur lors de la soumission du signalement');
        this.submitting.set(false);
      }
    });
  }

  getMotifLabel(motif: string): string {
    const found = this.motifs.find(m => m.value === motif);
    return found ? found.label : motif;
  }

  getNiveauRisqueClass(niveau: string | null): string {
    const found = this.niveauxRisque.find(n => n.value === niveau);
    return found ? found.class : 'bg-slate-100 text-slate-800';
  }

  getStatutLabel(statut: string | null): string {
    const labels: Record<string, string> = {
      'SOUMISE': 'Soumise',
      'EN_COURS_ANALYSE': 'En cours d\'analyse',
      'CONFIRMEE': 'Fraude confirmée',
      'INFONDEE': 'Infondée',
      'ENQUETE_SUPPLEMENTAIRE': 'Enquête supplémentaire',
      'CLOTUREE': 'Clôturée'
    };
    return statut ? (labels[statut] || statut) : '';
  }

  getStatutClass(statut: string | null): string {
    const classes: Record<string, string> = {
      'SOUMISE': 'bg-amber-100 text-amber-800',
      'EN_COURS_ANALYSE': 'bg-blue-100 text-blue-800',
      'CONFIRMEE': 'bg-red-100 text-red-800',
      'INFONDEE': 'bg-green-100 text-green-800',
      'ENQUETE_SUPPLEMENTAIRE': 'bg-purple-100 text-purple-800',
      'CLOTUREE': 'bg-slate-100 text-slate-800'
    };
    return statut ? (classes[statut] || 'bg-slate-100 text-slate-800') : '';
  }
}
