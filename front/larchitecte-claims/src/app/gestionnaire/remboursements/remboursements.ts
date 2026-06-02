import { Component, OnInit, inject, signal, DestroyRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { ReimbursementService, ReimbursementResponse, CalculIndemnisationResponse, PropositionIndemnisationResponse, RemboursementStats } from '../../core/reimbursement.service';
import { ClaimService, ClaimResponse } from '../../core/claim.service';

@Component({
  selector: 'app-remboursements-gestionnaire',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './remboursements.html'
})
export class RemboursementsGestionnaire implements OnInit {
  private destroyRef = inject(DestroyRef);
  private rembService = inject(ReimbursementService);
  private claimService = inject(ClaimService);

  reimbursements = signal<ReimbursementResponse[]>([]);
  loading = signal(true);
  selectedRem = signal<ReimbursementResponse | null>(null);
  proposition = signal<PropositionIndemnisationResponse | null>(null);
  motifRefus = '';
  filterStatut = '';
  showCalculForm = signal(false);
  calculLoading = signal(false);
  calculResult = signal<CalculIndemnisationResponse | null>(null);
  showCreateForm = signal(false);
  createLoading = signal(false);
  stats = signal<RemboursementStats | null>(null);
  claims = signal<ClaimResponse[]>([]);

  calculClaimId = '';
  calculDegats = 0;
  calculCapital = 0;
  calculFranchise = 0;
  calculPlafond = 0;
  calculTaux = 0.9;
  calculType = 'accident';

  createClaimId = '';
  createDegats = 0;
  createCapital = 0;
  createFranchise = 0;
  createPlafond = 0;
  createTaux = 0.9;
  createType = 'accident';
  createMontantPropose = 0;
  createJustification = '';
  createNotes = '';

  stripeCheckoutLoading = signal(false);
  showConditions = signal(false);

  ngOnInit(): void {
    this.rembService.getAllReimbursements()
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (data) => { this.reimbursements.set(data); this.loading.set(false); },
        error: () => this.loading.set(false)
      });
    this.rembService.getStats()
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({ next: (s) => this.stats.set(s) });
  }

  get filteredReimbursements(): ReimbursementResponse[] {
    const rems = this.reimbursements();
    if (!this.filterStatut) return rems;
    return rems.filter(r => r.statut === this.filterStatut);
  }

  get totalApprouve(): number {
    return this.reimbursements().filter((r: ReimbursementResponse) => r.statut === 'VALIDEE' || r.statut === 'PAYE').reduce((sum: number, r: ReimbursementResponse) => sum + r.montantFinal, 0);
  }

  get totalEnAttente(): number {
    return this.reimbursements().filter((r: ReimbursementResponse) => r.statut === 'EN_ATTENTE').reduce((sum: number, r: ReimbursementResponse) => sum + r.montantPropose, 0);
  }

  get totalRefuse(): number {
    return this.reimbursements().filter((r: ReimbursementResponse) => r.statut === 'REFUSE').reduce((sum: number, r: ReimbursementResponse) => sum + r.montantPropose, 0);
  }

  getStatutLabel(statut: string): string { return this.rembService.getStatutLabel(statut as any); }
  getStatutColor(statut: string): string { return this.rembService.getStatutColor(statut as any); }
  getMethodeLabel(m: string): string { return this.rembService.getMethodeLabel(m as any); }

  // Calcul automatisé
  openCalculForm(): void {
    this.showCalculForm.set(true);
    this.calculResult.set(null);
    this.loadClaims();
  }

  loadClaims(): void {
    this.claimService.getAllClaims()
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({ next: (data) => this.claims.set(data) });
  }

  onClaimSelected(claimId: string, formType: 'calcul' | 'create'): void {
    const claim = this.claims().find(c => c.id === claimId);
    if (!claim) return;
    if (formType === 'calcul') {
      this.calculClaimId = claimId;
      this.calculType = claim.categorie || claim.type || 'accident';
      this.calculFranchise = claim.franchise || 0;
      this.calculPlafond = claim.plafondCouverture || 0;
      const est = parseFloat(claim.estimation) || 0;
      this.calculDegats = est;
    } else {
      this.createClaimId = claimId;
      this.createType = claim.categorie || claim.type || 'accident';
      this.createFranchise = claim.franchise || 0;
      this.createPlafond = claim.plafondCouverture || 0;
      const est = parseFloat(claim.estimation) || 0;
      this.createDegats = est;
    }
  }
  closeCalculForm(): void { this.showCalculForm.set(false); }

  calculerIndemnisation(): void {
    this.calculLoading.set(true);
    this.rembService.calculerIndemnisation({
      claimId: this.calculClaimId,
      montantDegats: this.calculDegats,
      capitalAssure: this.calculCapital,
      franchise: this.calculFranchise,
      plafondGarantie: this.calculPlafond,
      tauxRemboursement: this.calculTaux,
      typeSinistre: this.calculType
    }).pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: (res) => { this.calculResult.set(res); this.calculLoading.set(false); },
      error: () => this.calculLoading.set(false)
    });
  }

  // Création remboursement
  openCreateForm(): void {
    this.showCreateForm.set(true);
    this.loadClaims();
    if (this.calculResult()) {
      this.createClaimId = this.calculClaimId;
      this.createDegats = this.calculDegats;
      this.createCapital = this.calculCapital;
      this.createFranchise = this.calculFranchise;
      this.createPlafond = this.calculPlafond;
      this.createTaux = this.calculTaux;
      this.createType = this.calculType;
      this.createMontantPropose = this.calculResult()!.montantFinalCalcule;
    }
  }
  closeCreateForm(): void { this.showCreateForm.set(false); }

  createReimbursement(): void {
    this.createLoading.set(true);
    this.rembService.createReimbursement({
      claimId: this.createClaimId,
      montantDegats: this.createDegats,
      capitalAssure: this.createCapital,
      franchise: this.createFranchise,
      plafondGarantie: this.createPlafond,
      tauxRemboursement: this.createTaux,
      typeSinistre: this.createType,
      montantPropose: this.createMontantPropose,
      methodePaiement: 'CARTE_BANCAIRE',
      justification: this.createJustification,
      notes: this.createNotes
    }).pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: () => { this.createLoading.set(false); this.showCreateForm.set(false); this.refreshList(); },
      error: () => this.createLoading.set(false)
    });
  }

  // Proposition détaillée
  viewProposition(rem: ReimbursementResponse): void {
    this.rembService.getPropositionDetaillee(rem.id)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({ next: (p) => this.proposition.set(p) });
  }
  closeProposition(): void { this.proposition.set(null); }

  formatDate(dateString: string): string {
    if (!dateString) return 'N/A';
    return new Date(dateString).toLocaleDateString('fr-FR', { day: 'numeric', month: 'long', year: 'numeric' });
  }

  openDetail(rem: ReimbursementResponse): void {
    this.selectedRem.set(rem);
    this.motifRefus = '';
  }

  closeDetail(): void {
    this.selectedRem.set(null);
  }

  refuseRem(rem: ReimbursementResponse): void {
    if (!this.motifRefus) return;
    this.rembService.refuseReimbursement(rem.id, this.motifRefus)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: () => this.refreshList(),
        error: (err) => console.error('Erreur refus', err)
      });
  }

  processRem(rem: ReimbursementResponse): void {
    this.stripeCheckoutLoading.set(true);
    this.rembService.createStripeCheckoutSession(rem.id)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (res) => {
          this.stripeCheckoutLoading.set(false);
          // Rediriger vers la page de paiement Stripe
          window.open(res.url, '_blank');
          this.refreshList();
        },
        error: (err) => {
          this.stripeCheckoutLoading.set(false);
          console.error('Stripe error detail:', err.error);
          console.error('Status:', err.status);
          console.error('Erreur création session Stripe', err);
        }
      });
  }

  confirmPayment(rem: ReimbursementResponse): void {
    this.rembService.confirmPayment(rem.id)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: () => this.refreshList(),
        error: (err) => console.error('Erreur paiement', err)
      });
  }

  private refreshList(): void {
    this.selectedRem.set(null);
    this.proposition.set(null);
    this.rembService.getAllReimbursements()
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({ next: (data) => this.reimbursements.set(data) });
    this.rembService.getStats()
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({ next: (s) => this.stats.set(s) });
  }
}
