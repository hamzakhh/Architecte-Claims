import { Component, inject, signal, computed, OnInit } from '@angular/core';
import { ClaimService, ClaimResponse, ExpertProfileResponse, ClaimHistoryEntry, QualificationRequest, IndemnisationRequest } from '../../core/claim.service';
import { ExpertiseService, ExpertiseResponse } from '../../core/expertise.service';
import { FileService } from '../../core/file.service';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { getStatutLabel, getStatutClass, getExpertiseStatutLabel, getExpertiseStatutClass, getTypeLabel, getGraviteLabel, getGraviteClass, formatDate, formatDateTime, getSpecialiteLabel } from '../../shared/gestionnaire-helpers';

@Component({
  selector: 'app-dossiers-assignes',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './dossiersassignes.html'
})
export class DossiersAssignes implements OnInit {
  private claimService = inject(ClaimService);
  private expertiseService = inject(ExpertiseService);
  private fileService = inject(FileService);

  claims = signal<ClaimResponse[]>([]);
  experts = signal<ExpertProfileResponse[]>([]);
  gestionnaires = signal<ExpertProfileResponse[]>([]);
  loading = signal(true);
  error = signal('');
  success = signal('');

  // Search & filter
  searchTerm = signal('');
  statusFilter = signal<string>('all');
  filteredClaims = computed(() => {
    let result = this.claims();
    const term = this.searchTerm().toLowerCase();
    if (term) {
      result = result.filter(c =>
        (c.id || '').toLowerCase().includes(term) ||
        (c.reference || '').toLowerCase().includes(term) ||
        (c.assureNom || '').toLowerCase().includes(term) ||
        (c.type || '').toLowerCase().includes(term) ||
        (c.lieu || '').toLowerCase().includes(term)
      );
    }
    const status = this.statusFilter();
    if (status !== 'all') {
      result = result.filter(c => c.statut === status);
    }
    return result;
  });

  // Stats
  totalDossiers = computed(() => this.claims().length);
  enCoursCount = computed(() => this.claims().filter(c => c.statut === 'EN_COURS' || c.statut === 'EN_REVISION').length);
  expertiseCount = computed(() => this.claims().filter(c => c.statut === 'EXPERTISE').length);
  clotureCount = computed(() => this.claims().filter(c => c.statut === 'CLOTURE' || c.statut === 'VALIDE').length);

  // Assign expert modal
  selectedClaimId = signal<string | null>(null);
  selectedExpertId = signal<string | null>(null);
  showAssignModal = signal(false);
  assignLoading = signal(false);

  // Notes modal
  showNotesModal = signal(false);
  notesClaimId = signal<string | null>(null);
  notesText = signal('');


  // Transfer modal
  showTransferModal = signal(false);
  transferClaimId = signal<string | null>(null);
  transferGestionnaireId = signal<string | null>(null);

  // Detail panel
  selectedClaim = signal<ClaimResponse | null>(null);
  claimExpertises = signal<ExpertiseResponse[]>([]);
  claimHistory = signal<ClaimHistoryEntry[]>([]);
  detailLoading = signal(false);

  // Qualification modal (2.2.1)
  showQualificationModal = signal(false);
  qualificationClaimId = signal<string | null>(null);
  qualificationGravite = signal<string>('MODEREE');
  qualificationCouverture = signal('');
  qualificationFranchise = signal<number | null>(null);
  qualificationPlafond = signal<number | null>(null);

  // Indemnisation modal (2.2.4)
  showIndemnisationModal = signal(false);
  indemnisationClaimId = signal<string | null>(null);
  indemnisationMontant = signal<number | null>(null);
  indemnisationMotif = signal('');

  // Refus indemnisation modal
  showRefusIndemnisationModal = signal(false);
  refusIndemnisationClaimId = signal<string | null>(null);
  refusIndemnisationMotif = signal('');

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
    this.claimService.getExperts().subscribe({
      next: (data) => this.experts.set(data),
      error: () => {}
    });
    this.claimService.getGestionnaires().subscribe({
      next: (data) => this.gestionnaires.set(data),
      error: () => {}
    });
  }

  onSearchInput(event: Event): void {
    this.searchTerm.set((event.target as HTMLInputElement).value);
  }

  setStatusFilter(filter: string): void {
    this.statusFilter.set(filter);
  }

  // Detail panel
  openDetail(claim: ClaimResponse): void {
    this.selectedClaim.set(claim);
    this.detailLoading.set(true);
    this.expertiseService.getExpertisesByClaim(claim.id).subscribe({
      next: (data) => {
        this.claimExpertises.set(data);
        this.detailLoading.set(false);
      },
      error: () => {
        this.claimExpertises.set([]);
        this.detailLoading.set(false);
      }
    });
    this.claimService.getClaimHistory(claim.id).subscribe({
      next: (data) => this.claimHistory.set(data),
      error: () => this.claimHistory.set([])
    });
  }

  closeDetail(): void {
    this.selectedClaim.set(null);
    this.claimExpertises.set([]);
    this.claimHistory.set([]);
  }

  // Assign expert
  openAssignExpert(claimId: string): void {
    this.selectedClaimId.set(claimId);
    this.selectedExpertId.set(null);
    this.showAssignModal.set(true);
  }

  closeAssignModal(): void {
    this.showAssignModal.set(false);
    this.selectedClaimId.set(null);
    this.selectedExpertId.set(null);
  }

  confirmAssignExpert(): void {
    const claimId = this.selectedClaimId();
    const expertId = this.selectedExpertId();
    if (!claimId || !expertId) return;

    this.assignLoading.set(true);
    this.claimService.assignExpert(claimId, expertId).subscribe({
      next: () => {
        this.assignLoading.set(false);
        this.closeAssignModal();
        this.success.set('Expert assigné avec succès');
        this.loadData();
        this.clearSuccessAfterDelay();
      },
      error: () => {
        this.assignLoading.set(false);
        this.error.set('Erreur lors de l\'assignation de l\'expert');
      }
    });
  }

  autoAssignExpert(claimId: string): void {
    this.claimService.autoAssignExpert(claimId).subscribe({
      next: () => {
        this.success.set('Expert auto-assigné avec succès');
        this.loadData();
        this.clearSuccessAfterDelay();
      },
      error: () => this.error.set('Erreur lors de l\'assignation automatique')
    });
  }

  updateStatut(claimId: string, statut: string): void {
    this.claimService.updateClaimStatus(claimId, statut).subscribe({
      next: () => {
        this.success.set('Statut mis à jour avec succès');
        this.loadData();
        // Refresh detail panel if open
        const selected = this.selectedClaim();
        if (selected && selected.id === claimId) {
          this.claimService.getClaimById(claimId).subscribe({
            next: (updated) => this.selectedClaim.set(updated)
          });
        }
        this.clearSuccessAfterDelay();
      },
      error: () => this.error.set('Erreur lors de la mise à jour du statut')
    });
  }

  validerExpertise(expertiseId: string): void {
    this.expertiseService.validerExpertise(expertiseId).subscribe({
      next: () => {
        this.success.set('Expertise validée avec succès');
        const claim = this.selectedClaim();
        if (claim) {
          this.expertiseService.getExpertisesByClaim(claim.id).subscribe({
            next: (data) => this.claimExpertises.set(data)
          });
          this.claimService.getClaimById(claim.id).subscribe({
            next: (updated) => this.selectedClaim.set(updated)
          });
        }
        this.loadData();
        this.clearSuccessAfterDelay();
      },
      error: () => this.error.set('Erreur lors de la validation de l\'expertise')
    });
  }

  refuserExpertise(expertiseId: string): void {
    this.expertiseService.refuserExpertise(expertiseId).subscribe({
      next: () => {
        this.success.set('Expertise refusée');
        const claim = this.selectedClaim();
        if (claim) {
          this.expertiseService.getExpertisesByClaim(claim.id).subscribe({
            next: (data) => this.claimExpertises.set(data)
          });
          this.claimService.getClaimById(claim.id).subscribe({
            next: (updated) => this.selectedClaim.set(updated)
          });
        }
        this.loadData();
        this.clearSuccessAfterDelay();
      },
      error: () => this.error.set('Erreur lors du refus de l\'expertise')
    });
  }

  // Archive
  archiverSinistre(claimId: string): void {
    this.claimService.archiverSinistre(claimId).subscribe({
      next: () => {
        this.success.set('Sinistre archivé');
        this.loadData();
        this.closeDetail();
        this.clearSuccessAfterDelay();
      },
      error: () => this.error.set('Erreur lors de l\'archivage')
    });
  }

  // Notes modal
  openNotesModal(claimId: string): void {
    this.notesClaimId.set(claimId);
    const claim = this.claims().find(c => c.id === claimId);
    this.notesText.set(claim?.notesInternes || '');
    this.showNotesModal.set(true);
  }

  closeNotesModal(): void {
    this.showNotesModal.set(false);
    this.notesClaimId.set(null);
    this.notesText.set('');
  }

  saveNotes(): void {
    const claimId = this.notesClaimId();
    if (!claimId) return;
    this.claimService.updateNotesInternes(claimId, this.notesText()).subscribe({
      next: () => {
        this.success.set('Notes internes mises à jour');
        this.loadData();
        this.refreshDetailIfOpen(claimId);
        this.closeNotesModal();
        this.clearSuccessAfterDelay();
      },
      error: () => this.error.set('Erreur lors de la mise à jour des notes')
    });
  }


  // Transfer modal
  openTransferModal(claimId: string): void {
    this.transferClaimId.set(claimId);
    this.transferGestionnaireId.set(null);
    this.showTransferModal.set(true);
  }

  closeTransferModal(): void {
    this.showTransferModal.set(false);
    this.transferClaimId.set(null);
    this.transferGestionnaireId.set(null);
  }

  confirmerTransfert(): void {
    const claimId = this.transferClaimId();
    const gestionnaireId = this.transferGestionnaireId();
    if (!claimId || !gestionnaireId) return;
    this.claimService.transfererGestionnaire(claimId, gestionnaireId).subscribe({
      next: () => {
        this.success.set('Dossier transféré avec succès');
        this.loadData();
        this.closeDetail();
        this.closeTransferModal();
        this.clearSuccessAfterDelay();
      },
      error: () => this.error.set('Erreur lors du transfert')
    });
  }

  // Helpers
  refreshDetailIfOpen(claimId: string): void {
    const selected = this.selectedClaim();
    if (selected && selected.id === claimId) {
      this.claimService.getClaimById(claimId).subscribe({
        next: (updated) => this.selectedClaim.set(updated)
      });
    }
  }

  clearSuccessAfterDelay(): void {
    setTimeout(() => this.success.set(''), 3000);
  }

  getExpertDossierCount(expertId: string): number {
    return this.claims().filter(c => c.expertId === expertId && c.statut !== 'CLOTURE' && c.statut !== 'REFUSE').length;
  }

  getStatutLabel = getStatutLabel;
  getStatutClass = getStatutClass;
  getTypeLabel = getTypeLabel;
  getGraviteLabel = getGraviteLabel;
  getGraviteClass = getGraviteClass;
  getExpertiseStatutLabel = getExpertiseStatutLabel;
  getExpertiseStatutClass = getExpertiseStatutClass;
  formatDate = formatDate;
  formatDateTime = formatDateTime;

  getSpecialiteLabel = getSpecialiteLabel;

  getExpertProfile(expertId: string | null | undefined): ExpertProfileResponse | null {
    if (!expertId) return null;
    return this.experts().find(e => e.id === expertId) || null;
  }

  // ==================== Qualification (2.2.1) ====================

  openQualificationModal(claimId: string): void {
    const claim = this.claims().find(c => c.id === claimId);
    this.qualificationClaimId.set(claimId);
    this.qualificationGravite.set(claim?.gravite || 'MODEREE');
    this.qualificationCouverture.set(claim?.couvertureContractuelle || '');
    this.qualificationFranchise.set(claim?.franchise ?? null);
    this.qualificationPlafond.set(claim?.plafondCouverture ?? null);
    this.showQualificationModal.set(true);
  }

  submitQualification(): void {
    const claimId = this.qualificationClaimId();
    if (!claimId) return;
    const request: QualificationRequest = {
      gravite: this.qualificationGravite(),
      couvertureContractuelle: this.qualificationCouverture() || undefined,
      franchise: this.qualificationFranchise() ?? undefined,
      plafondCouverture: this.qualificationPlafond() ?? undefined
    };
    this.claimService.qualifierSinistre(claimId, request).subscribe({
      next: (updated) => {
        this.updateClaimInList(updated);
        if (this.selectedClaim()?.id === claimId) this.selectedClaim.set(updated);
        this.showQualificationModal.set(false);
        this.success.set('Sinistre qualifié avec succès');
        this.clearSuccessAfterDelay();
      },
      error: () => {
        this.error.set('Erreur lors de la qualification');
        this.clearSuccessAfterDelay();
      }
    });
  }

  // ==================== Indemnisation (2.2.4) ====================

  openIndemnisationModal(claimId: string): void {
    this.indemnisationClaimId.set(claimId);
    this.indemnisationMontant.set(null);
    this.indemnisationMotif.set('');
    this.showIndemnisationModal.set(true);
  }

  submitIndemnisation(): void {
    const claimId = this.indemnisationClaimId();
    const montant = this.indemnisationMontant();
    if (!claimId || montant === null) return;
    const request: IndemnisationRequest = {
      montantPropose: montant,
      motifIndemnisation: this.indemnisationMotif() || undefined
    };
    this.claimService.proposerIndemnisation(claimId, request).subscribe({
      next: (updated) => {
        this.updateClaimInList(updated);
        if (this.selectedClaim()?.id === claimId) this.selectedClaim.set(updated);
        this.showIndemnisationModal.set(false);
        this.success.set('Proposition d\'indemnisation envoyée');
        this.clearSuccessAfterDelay();
      },
      error: () => {
        this.error.set('Erreur lors de la proposition d\'indemnisation');
        this.clearSuccessAfterDelay();
      }
    });
  }

  accepterIndemnisation(claimId: string): void {
    this.claimService.accepterIndemnisation(claimId).subscribe({
      next: (updated) => {
        this.updateClaimInList(updated);
        if (this.selectedClaim()?.id === claimId) this.selectedClaim.set(updated);
        this.success.set('Indemnisation acceptée');
        this.clearSuccessAfterDelay();
      },
      error: () => {
        this.error.set('Erreur lors de l\'acceptation');
        this.clearSuccessAfterDelay();
      }
    });
  }

  openRefusIndemnisationModal(claimId: string): void {
    this.refusIndemnisationClaimId.set(claimId);
    this.refusIndemnisationMotif.set('');
    this.showRefusIndemnisationModal.set(true);
  }

  submitRefusIndemnisation(): void {
    const claimId = this.refusIndemnisationClaimId();
    if (!claimId) return;
    this.claimService.refuserIndemnisation(claimId, this.refusIndemnisationMotif()).subscribe({
      next: (updated) => {
        this.updateClaimInList(updated);
        if (this.selectedClaim()?.id === claimId) this.selectedClaim.set(updated);
        this.showRefusIndemnisationModal.set(false);
        this.success.set('Indemnisation refusée — recours en cours');
        this.clearSuccessAfterDelay();
      },
      error: () => {
        this.error.set('Erreur lors du refus d\'indemnisation');
        this.clearSuccessAfterDelay();
      }
    });
  }

  initierPaiement(claimId: string): void {
    this.claimService.initierPaiement(claimId).subscribe({
      next: (updated) => {
        this.updateClaimInList(updated);
        if (this.selectedClaim()?.id === claimId) this.selectedClaim.set(updated);
        this.success.set('Paiement initié');
        this.clearSuccessAfterDelay();
      },
      error: () => {
        this.error.set('Erreur lors de l\'initiation du paiement');
        this.clearSuccessAfterDelay();
      }
    });
  }

  confirmerPaiement(claimId: string): void {
    this.claimService.confirmerPaiement(claimId).subscribe({
      next: (updated) => {
        this.updateClaimInList(updated);
        if (this.selectedClaim()?.id === claimId) this.selectedClaim.set(updated);
        this.success.set('Paiement confirmé — dossier clôturé');
        this.clearSuccessAfterDelay();
      },
      error: () => {
        this.error.set('Erreur lors de la confirmation du paiement');
        this.clearSuccessAfterDelay();
      }
    });
  }

  declarerRecours(claimId: string): void {
    this.claimService.declarerRecours(claimId).subscribe({
      next: (updated) => {
        this.updateClaimInList(updated);
        if (this.selectedClaim()?.id === claimId) this.selectedClaim.set(updated);
        this.success.set('Recours déclaré');
        this.clearSuccessAfterDelay();
      },
      error: () => {
        this.error.set('Erreur lors de la déclaration de recours');
        this.clearSuccessAfterDelay();
      }
    });
  }

  updateClaimInList(updated: ClaimResponse): void {
    this.claims.update(list => list.map(c => c.id === updated.id ? updated : c));
  }

  getFileIcon(filename: string): string {
    const ext = filename.split('.').pop()?.toLowerCase();
    if (ext === 'pdf') return 'picture_as_pdf';
    if (['jpg', 'jpeg', 'png', 'gif', 'webp'].includes(ext || '')) return 'image';
    return 'description';
  }

  downloadFile(fileName: string): void {
    const url = this.fileService.getDownloadUrl(fileName);
    const token = localStorage.getItem('larchitecte_token');
    fetch(url, {
      headers: { 'Authorization': `Bearer ${token}` }
    })
      .then(res => res.blob())
      .then(blob => {
        const a = document.createElement('a');
        a.href = URL.createObjectURL(blob);
        a.download = fileName;
        a.click();
        URL.revokeObjectURL(a.href);
      })
      .catch(err => console.error('Erreur téléchargement fichier', err));
  }
}
