import { Component, inject, signal, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ClaimService, ClaimResponse } from '../../core/claim.service';
import { ExpertiseService, ExpertiseResponse } from '../../core/expertise.service';
import { AnalyseIAService, AnalyseIAResponse } from '../../core/analyse-ia.service';

@Component({
  selector: 'app-approbation',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './approbation.html'
})
export class Approbation implements OnInit {
  private claimService = inject(ClaimService);
  private expertiseService = inject(ExpertiseService);
  private analyseIAService = inject(AnalyseIAService);

  claims = signal<ClaimResponse[]>([]);
  selectedClaim = signal<ClaimResponse | null>(null);
  expertise = signal<ExpertiseResponse | null>(null);
  analyseIA = signal<AnalyseIAResponse | null>(null);
  loading = signal(true);
  actionLoading = signal(false);
  error = signal('');
  filter = signal<string>('all');

  ngOnInit(): void {
    this.loadClaims();
  }

  loadClaims(): void {
    this.loading.set(true);
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
  }

  selectClaim(claim: ClaimResponse): void {
    this.selectedClaim.set(claim);
    this.expertise.set(null);
    this.analyseIA.set(null);

    // Load expertise if claim is in EXPERTISE or VALIDE status
    if (claim.statut === 'EXPERTISE' || claim.statut === 'VALIDE' || claim.statut === 'EN_REVISION') {
      this.expertiseService.getExpertisesByClaim(claim.id).subscribe({
        next: (data) => {
          if (data.length > 0) this.expertise.set(data[data.length - 1]);
        },
        error: () => {}
      });
    }

    // Load AI analysis
    this.analyseIAService.getAnalyseByClaimId(claim.id).subscribe({
      next: (data) => this.analyseIA.set(data),
      error: () => {}
    });
  }

  validerSinistre(): void {
    const claim = this.selectedClaim();
    if (!claim) return;
    this.actionLoading.set(true);

    this.claimService.updateClaimStatus(claim.id, 'VALIDE').subscribe({
      next: () => {
        this.actionLoading.set(false);
        this.loadClaims();
        this.selectedClaim.set(null);
      },
      error: () => {
        this.error.set('Erreur lors de la validation');
        this.actionLoading.set(false);
      }
    });
  }

  refuserSinistre(): void {
    const claim = this.selectedClaim();
    if (!claim) return;
    this.actionLoading.set(true);

    this.claimService.updateClaimStatus(claim.id, 'REFUSE').subscribe({
      next: () => {
        this.actionLoading.set(false);
        this.loadClaims();
        this.selectedClaim.set(null);
      },
      error: () => {
        this.error.set('Erreur lors du refus');
        this.actionLoading.set(false);
      }
    });
  }

  validerExpertise(): void {
    const exp = this.expertise();
    if (!exp) return;
    this.actionLoading.set(true);

    this.expertiseService.validerExpertise(exp.id).subscribe({
      next: () => {
        this.actionLoading.set(false);
        this.loadClaims();
        this.selectedClaim.set(null);
      },
      error: () => {
        this.error.set('Erreur lors de la validation de l\'expertise');
        this.actionLoading.set(false);
      }
    });
  }

  refuserExpertise(): void {
    const exp = this.expertise();
    if (!exp) return;
    this.actionLoading.set(true);

    this.expertiseService.refuserExpertise(exp.id).subscribe({
      next: () => {
        this.actionLoading.set(false);
        this.loadClaims();
        this.selectedClaim.set(null);
      },
      error: () => {
        this.error.set('Erreur lors du refus de l\'expertise');
        this.actionLoading.set(false);
      }
    });
  }

  assignerExpert(): void {
    const claim = this.selectedClaim();
    if (!claim) return;
    this.actionLoading.set(true);

    this.claimService.autoAssignExpert(claim.id).subscribe({
      next: () => {
        this.actionLoading.set(false);
        this.loadClaims();
        this.selectClaim(claim);
      },
      error: () => {
        this.error.set('Erreur lors de l\'assignation de l\'expert');
        this.actionLoading.set(false);
      }
    });
  }

  getFilteredClaims(): ClaimResponse[] {
    const f = this.filter();
    if (f === 'all') return this.claims();
    return this.claims().filter(c => c.statut === f);
  }

  getStatutLabel(statut: string): string {
    const labels: Record<string, string> = {
      'EN_COURS': 'En cours',
      'EN_REVISION': 'En révision',
      'EXPERTISE': 'Expertise',
      'VALIDE': 'Validé',
      'REFUSE': 'Refusé',
      'INDEMNISATION_PROPOSEE': 'Indemnisation proposée',
      'INDEMNISATION_ACCEPTEE': 'Indemnisation acceptée',
      'PAIEMENT_EN_COURS': 'Paiement en cours',
      'RECOURS': 'Recours',
      'CLOTURE': 'Clôturé',
      'ARCHIVE': 'Archivé'
    };
    return labels[statut] || statut;
  }

  getStatutClass(statut: string): string {
    const classes: Record<string, string> = {
      'EN_COURS': 'bg-blue-100 text-blue-800',
      'EN_REVISION': 'bg-amber-100 text-amber-800',
      'EXPERTISE': 'bg-purple-100 text-purple-800',
      'VALIDE': 'bg-green-100 text-green-800',
      'REFUSE': 'bg-red-100 text-red-800',
      'INDEMNISATION_PROPOSEE': 'bg-teal-100 text-teal-800',
      'CLOTURE': 'bg-slate-100 text-slate-800'
    };
    return classes[statut] || 'bg-slate-100 text-slate-800';
  }
}
