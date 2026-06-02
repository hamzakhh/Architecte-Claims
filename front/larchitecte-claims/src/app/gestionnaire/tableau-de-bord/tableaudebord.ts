import { Component, inject, signal, OnInit } from '@angular/core';
import { ClaimService, DashboardStatsResponse, ClaimResponse } from '../../core/claim.service';
import { ExpertiseService, ExpertiseResponse } from '../../core/expertise.service';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { getStatutLabel, getStatutClass } from '../../shared/gestionnaire-helpers';

@Component({
  selector: 'app-tableau-de-bord',
  standalone: true,
  imports: [CommonModule, RouterLink],
  templateUrl: './tableaudebord.html'
})
export class TableauDeBordGestionnaire implements OnInit {
  private claimService = inject(ClaimService);
  private expertiseService = inject(ExpertiseService);

  stats = signal<DashboardStatsResponse | null>(null);
  expertisesEnAttente = signal<ExpertiseResponse[]>([]);
  loading = signal(true);
  error = signal('');

  ngOnInit(): void {
    this.loadDashboard();
  }

  loadDashboard(): void {
    this.loading.set(true);
    this.error.set('');

    this.claimService.getDashboardStats().subscribe({
      next: (data) => {
        this.stats.set(data);
        this.loading.set(false);
      },
      error: (err) => {
        this.error.set('Erreur lors du chargement des statistiques');
        this.loading.set(false);
      }
    });

    this.expertiseService.getExpertisesByGestionnaire().subscribe({
      next: (data) => {
        this.expertisesEnAttente.set(data.filter(e => e.statut === 'SOUMISE'));
      },
      error: () => {}
    });
  }

  validerExpertise(id: string): void {
    this.expertiseService.validerExpertise(id).subscribe({
      next: () => this.loadDashboard(),
      error: () => this.error.set('Erreur lors de la validation')
    });
  }

  refuserExpertise(id: string): void {
    this.expertiseService.refuserExpertise(id).subscribe({
      next: () => this.loadDashboard(),
      error: () => this.error.set('Erreur lors du refus')
    });
  }

  getStatutLabel = getStatutLabel;
  getStatutClass = getStatutClass;
}
