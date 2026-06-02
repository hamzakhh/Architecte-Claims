import { Component, inject, signal, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { AnalyseIAService, AnalyseIAResponse, AnalyseIAStats } from '../../core/analyse-ia.service';
import { ClaimService, ClaimResponse } from '../../core/claim.service';

@Component({
  selector: 'app-analyse-ia',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './analyseia.html'
})
export class AnalyseIA implements OnInit {
  private analyseIAService = inject(AnalyseIAService);
  private claimService = inject(ClaimService);

  analyses = signal<AnalyseIAResponse[]>([]);
  claims = signal<ClaimResponse[]>([]);
  stats = signal<AnalyseIAStats | null>(null);
  selectedClaim = signal<ClaimResponse | null>(null);
  selectedAnalyse = signal<AnalyseIAResponse | null>(null);
  loading = signal(true);
  analysing = signal(false);
  error = signal('');

  ngOnInit(): void {
    this.loadData();
  }

  loadData(): void {
    this.loading.set(true);
    this.error.set('');

    this.claimService.getAllClaims().subscribe({
      next: (data) => {
        this.claims.set(data.filter(c => c.statut === 'EN_COURS' || c.statut === 'EN_REVISION'));
        this.loading.set(false);
      },
      error: () => {
        this.error.set('Erreur lors du chargement des sinistres');
        this.loading.set(false);
      }
    });

    this.analyseIAService.getStats().subscribe({
      next: (data) => this.stats.set(data),
      error: () => {}
    });

    this.analyseIAService.getAnalysesNecessitantExpert().subscribe({
      next: (data) => this.analyses.set(data),
      error: () => {}
    });
  }

  selectClaim(claim: ClaimResponse): void {
    this.selectedClaim.set(claim);
    this.selectedAnalyse.set(null);

    if (claim.analyseIAId) {
      this.analyseIAService.getAnalyseByClaimId(claim.id).subscribe({
        next: (data) => this.selectedAnalyse.set(data),
        error: () => {}
      });
    }
  }

  analyserSinistre(claim: ClaimResponse): void {
    this.analysing.set(true);
    this.error.set('');

    const typeAnalyse = this.selectedAnalyse() ? 'APPROFONDIE' : 'INITIALE';

    this.analyseIAService.analyserSinistre({ claimId: claim.id, typeAnalyse }).subscribe({
      next: (data) => {
        this.selectedAnalyse.set(data);
        this.analysing.set(false);
        this.loadData();
      },
      error: (err) => {
        this.error.set('Erreur lors de l\'analyse IA');
        this.analysing.set(false);
      }
    });
  }

  getSeveriteClass(severite: string | null): string {
    switch (severite) {
      case 'CRITIQUE': return 'bg-red-100 text-red-800 border-red-200';
      case 'ELEVEE': return 'bg-orange-100 text-orange-800 border-orange-200';
      case 'MODEREE': return 'bg-amber-100 text-amber-800 border-amber-200';
      case 'FAIBLE': return 'bg-green-100 text-green-800 border-green-200';
      default: return 'bg-slate-100 text-slate-800 border-slate-200';
    }
  }

  getSeveriteIcon(severite: string | null): string {
    switch (severite) {
      case 'CRITIQUE': return 'dangerous';
      case 'ELEVEE': return 'warning';
      case 'MODEREE': return 'info';
      case 'FAIBLE': return 'check_circle';
      default: return 'help';
    }
  }

  getScoreColor(score: number): string {
    if (score >= 70) return 'text-red-600';
    if (score >= 50) return 'text-orange-600';
    if (score >= 30) return 'text-amber-600';
    return 'text-green-600';
  }

  getScoreBarColor(score: number): string {
    if (score >= 70) return 'bg-red-500';
    if (score >= 50) return 'bg-orange-500';
    if (score >= 30) return 'bg-amber-500';
    return 'bg-green-500';
  }
}
