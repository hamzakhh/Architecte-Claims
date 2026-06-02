import { Component, inject, signal, OnInit, computed } from '@angular/core';
import { ExpertiseService, ExpertiseResponse, ExpertiseRequest } from '../../core/expertise.service';
import { ClaimService, ClaimResponse } from '../../core/claim.service';
import { FileService } from '../../core/file.service';
import { CommonModule } from '@angular/common';
import { getExpertiseStatutLabel, getExpertiseStatutClass, getTypeLabel, formatDate, formatDateTime } from '../../shared/gestionnaire-helpers';

@Component({
  selector: 'app-rapports',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './rapports.html'
})
export class RapportsGestionnaire implements OnInit {
  private expertiseService = inject(ExpertiseService);
  private claimService = inject(ClaimService);
  private fileService = inject(FileService);

  expertises = signal<ExpertiseResponse[]>([]);
  claims = signal<ClaimResponse[]>([]);
  selectedExpertise = signal<ExpertiseResponse | null>(null);
  selectedClaim = signal<ClaimResponse | null>(null);
  loading = signal(true);
  error = signal('');
  saving = signal(false);
  success = signal('');

  // Filters
  filterStatut = signal('');
  searchTerm = signal('');

  // Modal states
  showCreateModal = signal(false);
  showDetailModal = signal(false);
  selectedClaimIdForExpertise = signal('');

  filteredExpertises = computed(() => {
    let result = this.expertises();
    const statut = this.filterStatut();
    const search = this.searchTerm().toLowerCase();

    if (statut) {
      result = result.filter(e => e.statut === statut);
    }
    if (search) {
      result = result.filter(e =>
        (e.claimReference?.toLowerCase().includes(search)) ||
        (e.expertNom?.toLowerCase().includes(search)) ||
        (e.conclusion?.toLowerCase().includes(search))
      );
    }
    return result;
  });

  statsEnAttente = computed(() => this.expertises().filter(e => e.statut === 'EN_ATTENTE' || e.statut === 'EN_COURS').length);
  statsSoumises = computed(() => this.expertises().filter(e => e.statut === 'SOUMISE').length);
  statsValidees = computed(() => this.expertises().filter(e => e.statut === 'VALIDEE').length);
  statsRefusees = computed(() => this.expertises().filter(e => e.statut === 'REFUSEE').length);

  ngOnInit(): void {
    this.loadData();
  }

  loadData(): void {
    this.loading.set(true);
    this.error.set('');
    this.success.set('');

    this.expertiseService.getExpertisesByGestionnaire().subscribe({
      next: (data) => {
        this.expertises.set(data);
        this.loading.set(false);
      },
      error: () => {
        this.error.set('Erreur lors du chargement des expertises');
        this.loading.set(false);
      }
    });

    this.claimService.getAssignedClaims().subscribe({
      next: (data) => this.claims.set(data),
      error: () => {}
    });
  }

  openDetail(expertise: ExpertiseResponse): void {
    this.selectedExpertise.set(expertise);
    this.showDetailModal.set(true);
    this.success.set('');
    this.error.set('');

    if (expertise.claimId) {
      this.claimService.getClaimById(expertise.claimId).subscribe({
        next: (claim) => this.selectedClaim.set(claim),
        error: () => {}
      });
    }
  }

  closeDetailModal(): void {
    this.showDetailModal.set(false);
    this.selectedExpertise.set(null);
    this.selectedClaim.set(null);
    this.success.set('');
    this.error.set('');
  }

  openCreateExpertise(): void {
    this.showCreateModal.set(true);
    this.selectedClaimIdForExpertise.set('');
    this.success.set('');
    this.error.set('');
  }

  closeCreateModal(): void {
    this.showCreateModal.set(false);
    this.selectedClaimIdForExpertise.set('');
  }

  confirmCreateExpertise(): void {
    const claimId = this.selectedClaimIdForExpertise();
    if (!claimId) return;

    this.saving.set(true);
    this.error.set('');

    const request: ExpertiseRequest = { claimId };

    this.expertiseService.createExpertise(request).subscribe({
      next: () => {
        this.saving.set(false);
        this.showCreateModal.set(false);
        this.success.set('Demande d\'expertise créée avec succès');
        this.loadData();
      },
      error: () => {
        this.saving.set(false);
        this.error.set('Erreur lors de la création de l\'expertise');
      }
    });
  }

  validerExpertise(expertiseId: string): void {
    this.saving.set(true);
    this.error.set('');
    this.success.set('');

    this.expertiseService.validerExpertise(expertiseId).subscribe({
      next: (updated) => {
        this.saving.set(false);
        this.success.set('Expertise validée avec succès');
        this.selectedExpertise.set(updated);
        this.loadData();
      },
      error: () => {
        this.saving.set(false);
        this.error.set('Erreur lors de la validation');
      }
    });
  }

  refuserExpertise(expertiseId: string): void {
    this.saving.set(true);
    this.error.set('');
    this.success.set('');

    this.expertiseService.refuserExpertise(expertiseId).subscribe({
      next: (updated) => {
        this.saving.set(false);
        this.success.set('Expertise refusée');
        this.selectedExpertise.set(updated);
        this.loadData();
      },
      error: () => {
        this.saving.set(false);
        this.error.set('Erreur lors du refus');
      }
    });
  }

  onFilterStatut(event: Event): void {
    const value = (event.target as HTMLSelectElement).value;
    this.filterStatut.set(value === '' ? '' : value);
  }

  onSearch(event: Event): void {
    const value = (event.target as HTMLInputElement).value;
    this.searchTerm.set(value);
  }

  resetFilters(): void {
    this.filterStatut.set('');
    this.searchTerm.set('');
  }

  getStatutLabel = getExpertiseStatutLabel;
  getStatutClass = getExpertiseStatutClass;

  getRecommandationLabel(rec: string | undefined): string {
    if (!rec) return 'N/A';
    const labels: Record<string, string> = {
      'ACCEPTER': 'Accepter le sinistre',
      'REFUSER': 'Refuser le sinistre',
      'COMPLEMENT': 'Demander des compléments'
    };
    return labels[rec] || rec;
  }

  getRecommandationClass(rec: string | undefined): string {
    if (!rec) return 'bg-slate-100 text-slate-600';
    const classes: Record<string, string> = {
      'ACCEPTER': 'bg-emerald-100 text-emerald-700',
      'REFUSER': 'bg-red-100 text-red-700',
      'COMPLEMENT': 'bg-amber-100 text-amber-700'
    };
    return classes[rec] || 'bg-slate-100 text-slate-600';
  }

  getTypeLabel = getTypeLabel;
  formatDate = formatDate;
  formatDateTime = formatDateTime;

  claimsWithoutExpertise(): ClaimResponse[] {
    const claimIdsWithExpertise = new Set(this.expertises().map(e => e.claimId));
    return this.claims().filter(c => !claimIdsWithExpertise.has(c.id));
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

  exportClaimsCsv(): void {
    const url = '/api/reports/claims/csv';
    const token = localStorage.getItem('larchitecte_token');
    fetch(url, {
      headers: { 'Authorization': `Bearer ${token}` }
    })
      .then(res => res.blob())
      .then(blob => {
        const a = document.createElement('a');
        a.href = URL.createObjectURL(blob);
        a.download = `sinistres_${new Date().toISOString().slice(0,10)}.csv`;
        a.click();
        URL.revokeObjectURL(a.href);
      })
      .catch(err => console.error('Erreur export CSV sinistres', err));
  }

  exportReimbursementsCsv(): void {
    const url = '/api/reports/reimbursements/csv';
    const token = localStorage.getItem('larchitecte_token');
    fetch(url, {
      headers: { 'Authorization': `Bearer ${token}` }
    })
      .then(res => res.blob())
      .then(blob => {
        const a = document.createElement('a');
        a.href = URL.createObjectURL(blob);
        a.download = `remboursements_${new Date().toISOString().slice(0,10)}.csv`;
        a.click();
        URL.revokeObjectURL(a.href);
      })
      .catch(err => console.error('Erreur export CSV remboursements', err));
  }
}
