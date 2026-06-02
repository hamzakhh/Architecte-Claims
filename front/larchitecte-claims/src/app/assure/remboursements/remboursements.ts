import { Component, OnInit, inject, signal, DestroyRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { ReimbursementService, ReimbursementResponse, PropositionIndemnisationResponse } from '../../core/reimbursement.service';

@Component({
  selector: 'app-remboursements',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './remboursements.html'
})
export class RemboursementsAssure implements OnInit {
  private destroyRef = inject(DestroyRef);
  private rembService = inject(ReimbursementService);

  loading = signal(true);
  selectedRem = signal<ReimbursementResponse | null>(null);
  proposition = signal<PropositionIndemnisationResponse | null>(null);
  motifRefus = signal('');
  showConditions = signal(false);

  ngOnInit(): void {
    this.rembService.loadMyReimbursements();
    setTimeout(() => this.loading.set(false), 1000);
  }

  get reimbursements(): ReimbursementResponse[] {
    return this.rembService.reimbursements();
  }

  getStatutLabel(statut: string): string { return this.rembService.getStatutLabel(statut as any); }
  getStatutColor(statut: string): string { return this.rembService.getStatutColor(statut as any); }
  getMethodeLabel(m: string): string { return this.rembService.getMethodeLabel(m as any); }

  formatDate(dateString: string): string {
    if (!dateString) return 'N/A';
    return new Date(dateString).toLocaleDateString('fr-FR', { day: 'numeric', month: 'long', year: 'numeric' });
  }

  openDetail(rem: ReimbursementResponse): void {
    this.selectedRem.set(rem);
    this.motifRefus.set('');
  }

  closeDetail(): void {
    this.selectedRem.set(null);
  }

  viewProposition(rem: ReimbursementResponse): void {
    this.rembService.getPropositionDetaillee(rem.id)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({ next: (p) => this.proposition.set(p) });
  }

  closeProposition(): void { this.proposition.set(null); }

  validateRem(rem: ReimbursementResponse): void {
    this.rembService.validateReimbursement(rem.id)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: () => { this.rembService.loadMyReimbursements(); this.selectedRem.set(null); this.proposition.set(null); },
        error: (err) => console.error('Erreur validation', err)
      });
  }

  refuseRem(rem: ReimbursementResponse): void {
    if (!this.motifRefus()) return;
    this.rembService.refuseReimbursement(rem.id, this.motifRefus())
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: () => { this.rembService.loadMyReimbursements(); this.selectedRem.set(null); this.proposition.set(null); },
        error: (err) => console.error('Erreur refus', err)
      });
  }

  get totalEnAttente(): number {
    return this.reimbursements.filter((r: ReimbursementResponse) => r.statut === 'EN_ATTENTE').length;
  }

  get totalPaye(): number {
    return this.reimbursements.filter((r: ReimbursementResponse) => r.statut === 'PAYE').reduce((sum: number, r: ReimbursementResponse) => sum + r.montantFinal, 0);
  }
}
