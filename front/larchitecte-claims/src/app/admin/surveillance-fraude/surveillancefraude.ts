import { Component, inject, signal, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { FraudAlertService, FraudAlertResponse, FraudAlertStatsResponse, FraudAlertResolutionRequest } from '../../core/fraud-alert.service';

@Component({
  selector: 'app-surveillance-fraude',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './surveillancefraude.html'
})
export class SurveillanceFraude implements OnInit {
  private fraudAlertService = inject(FraudAlertService);

  alerts = signal<FraudAlertResponse[]>([]);
  stats = signal<FraudAlertStatsResponse | null>(null);
  selectedAlert = signal<FraudAlertResponse | null>(null);
  loading = signal(true);
  actionLoading = signal(false);
  error = signal('');
  filter = signal<string>('all');

  // Résolution
  showResolveForm = signal(false);
  decision = signal('');
  notesResolution = signal('');

  decisions = [
    { value: 'confirme_fraude', label: 'Confirmer la fraude', class: 'bg-red-600 text-white' },
    { value: 'infonde', label: 'Signalement infondé', class: 'bg-green-600 text-white' },
    { value: 'enquete_supplementaire', label: 'Enquête supplémentaire', class: 'bg-purple-600 text-white' }
  ];

  ngOnInit(): void {
    this.loadData();
  }

  loadData(): void {
    this.loading.set(true);
    this.error.set('');

    this.fraudAlertService.getAllFraudAlerts().subscribe({
      next: (data) => {
        this.alerts.set(data);
        this.loading.set(false);
      },
      error: () => {
        this.error.set('Erreur lors du chargement des alertes');
        this.loading.set(false);
      }
    });

    this.fraudAlertService.getFraudAlertStats().subscribe({
      next: (data) => this.stats.set(data),
      error: () => {}
    });
  }

  selectAlert(alert: FraudAlertResponse): void {
    this.selectedAlert.set(alert);
    this.showResolveForm.set(false);
    this.decision.set('');
    this.notesResolution.set('');
  }

  startAnalysis(): void {
    const alert = this.selectedAlert();
    if (!alert) return;
    this.actionLoading.set(true);

    this.fraudAlertService.startAnalysis(alert.id).subscribe({
      next: (updated) => {
        this.actionLoading.set(false);
        this.selectedAlert.set(updated);
        this.loadData();
      },
      error: () => {
        this.error.set('Erreur lors du démarrage de l\'analyse');
        this.actionLoading.set(false);
      }
    });
  }

  openResolveForm(): void {
    this.showResolveForm.set(true);
    this.decision.set('');
    this.notesResolution.set('');
  }

  resolveAlert(): void {
    const alert = this.selectedAlert();
    if (!alert || !this.decision()) return;
    this.actionLoading.set(true);

    const request: FraudAlertResolutionRequest = {
      decision: this.decision(),
      notesResolution: this.notesResolution() || undefined
    };

    this.fraudAlertService.resolveFraudAlert(alert.id, request).subscribe({
      next: (updated) => {
        this.actionLoading.set(false);
        this.selectedAlert.set(updated);
        this.showResolveForm.set(false);
        this.loadData();
      },
      error: () => {
        this.error.set('Erreur lors de la résolution de l\'alerte');
        this.actionLoading.set(false);
      }
    });
  }

  getFilteredAlerts(): FraudAlertResponse[] {
    const f = this.filter();
    if (f === 'all') return this.alerts();
    if (f === 'pending') return this.alerts().filter(a => a.statut === 'SOUMISE' || a.statut === 'EN_COURS_ANALYSE' || a.statut === 'ENQUETE_SUPPLEMENTAIRE');
    if (f === 'resolved') return this.alerts().filter(a => a.statut === 'CONFIRMEE' || a.statut === 'INFONDEE' || a.statut === 'CLOTUREE');
    return this.alerts().filter(a => a.statut === f);
  }

  getMotifLabel(motif: string): string {
    const labels: Record<string, string> = {
      'documents_falsifies': 'Documents falsifiés',
      'incoherence_reclamations': 'Incohérence des réclamations',
      'surevaluation_degats': 'Surévaluation des dégâts',
      'sinistre_fictif': 'Sinistre fictif',
      'recurrence_suspecte': 'Récurrence suspecte',
      'autre': 'Autre'
    };
    return labels[motif] || motif;
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

  getNiveauRisqueClass(niveau: string | null): string {
    const classes: Record<string, string> = {
      'FAIBLE': 'bg-green-100 text-green-800',
      'MOYEN': 'bg-amber-100 text-amber-800',
      'ELEVE': 'bg-orange-100 text-orange-800',
      'CRITIQUE': 'bg-red-100 text-red-800'
    };
    return niveau ? (classes[niveau] || 'bg-slate-100 text-slate-800') : '';
  }

  getDecisionLabel(decision: string | null): string {
    const labels: Record<string, string> = {
      'confirme_fraude': 'Fraude confirmée',
      'infonde': 'Infondée',
      'enquete_supplementaire': 'Enquête supplémentaire'
    };
    return decision ? (labels[decision] || decision) : '';
  }
}
