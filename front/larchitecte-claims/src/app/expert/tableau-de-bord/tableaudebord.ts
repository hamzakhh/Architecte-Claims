import { Component, inject, signal, OnInit } from '@angular/core';
import { ClaimService, ExpertDashboardStatsResponse, ClaimResponse } from '../../core/claim.service';
import { ExpertiseService, ExpertiseResponse } from '../../core/expertise.service';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';

@Component({
  selector: 'app-tableau-de-bord',
  standalone: true,
  imports: [CommonModule, RouterLink],
  templateUrl: './tableaudebord.html'
})
export class TableauDeBordExpert implements OnInit {
  private claimService = inject(ClaimService);
  private expertiseService = inject(ExpertiseService);

  stats = signal<ExpertDashboardStatsResponse | null>(null);
  expertisesEnCours = signal<ExpertiseResponse[]>([]);
  recentClaims = signal<ClaimResponse[]>([]);
  loading = signal(true);
  error = signal('');

  ngOnInit(): void {
    this.loadDashboard();
  }

  loadDashboard(): void {
    this.loading.set(true);
    this.error.set('');

    this.claimService.getExpertDashboardStats().subscribe({
      next: (data) => {
        this.stats.set(data);
        this.loading.set(false);
      },
      error: () => {
        this.error.set('Erreur lors du chargement des statistiques');
        this.loading.set(false);
      }
    });

    this.expertiseService.getMyExpertises().subscribe({
      next: (data) => {
        this.expertisesEnCours.set(data.filter(e => e.statut === 'EN_COURS' || e.statut === 'EN_ATTENTE'));
      },
      error: () => {}
    });

    this.claimService.getExpertDossiers().subscribe({
      next: (data) => {
        this.recentClaims.set(data.slice(0, 5));
      },
      error: () => {}
    });
  }

  getTypeLabel(type: string): string {
    const labels: Record<string, string> = {
      'water': 'Dégât des Eaux',
      'fire': 'Incendie',
      'theft': 'Vol avec Effraction',
      'auto': 'Accident Auto',
      'natural': 'Catastrophe Naturelle'
    };
    return labels[type] || type;
  }

  getStatutLabel(statut: string): string {
    const labels: Record<string, string> = {
      'EN_COURS': 'En cours',
      'EN_REVISION': 'En révision',
      'EXPERTISE': 'En expertise',
      'VALIDE': 'Validé',
      'REFUSE': 'Refusé',
      'CLOTURE': 'Clôturé'
    };
    return labels[statut] || statut;
  }

  getStatutClass(statut: string): string {
    const classes: Record<string, string> = {
      'EN_COURS': 'bg-tertiary-fixed text-on-tertiary-fixed-variant',
      'EN_REVISION': 'bg-amber-100 text-amber-700',
      'EXPERTISE': 'bg-blue-100 text-blue-700',
      'VALIDE': 'bg-emerald-100 text-emerald-700',
      'REFUSE': 'bg-error-container text-on-error-container',
      'CLOTURE': 'bg-slate-100 text-slate-600'
    };
    return classes[statut] || 'bg-slate-100 text-slate-600';
  }

  getExpertiseStatutLabel(statut: string): string {
    const labels: Record<string, string> = {
      'EN_ATTENTE': 'En attente',
      'EN_COURS': 'En cours',
      'SOUMISE': 'Soumise',
      'VALIDEE': 'Validée',
      'REFUSEE': 'Refusée'
    };
    return labels[statut] || statut;
  }

  getExpertiseStatutClass(statut: string): string {
    const classes: Record<string, string> = {
      'EN_ATTENTE': 'bg-amber-100 text-amber-700',
      'EN_COURS': 'bg-blue-100 text-blue-700',
      'SOUMISE': 'bg-emerald-100 text-emerald-700',
      'VALIDEE': 'bg-emerald-200 text-emerald-800',
      'REFUSEE': 'bg-error-container text-on-error-container'
    };
    return classes[statut] || 'bg-slate-100 text-slate-600';
  }

  formatDate(dateStr: string | undefined): string {
    if (!dateStr) return '';
    const date = new Date(dateStr);
    return date.toLocaleDateString('fr-FR', { day: 'numeric', month: 'short', year: 'numeric' });
  }
}
