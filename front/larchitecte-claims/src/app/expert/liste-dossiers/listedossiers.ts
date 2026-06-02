import { Component, inject, signal, OnInit, computed } from '@angular/core';
import { ClaimService, ClaimResponse } from '../../core/claim.service';
import { ExpertiseService, ExpertiseResponse } from '../../core/expertise.service';
import { FileService } from '../../core/file.service';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';

@Component({
  selector: 'app-liste-dossiers',
  standalone: true,
  imports: [CommonModule, RouterLink],
  templateUrl: './listedossiers.html'
})
export class ListeDossiers implements OnInit {
  private claimService = inject(ClaimService);
  private expertiseService = inject(ExpertiseService);
  private fileService = inject(FileService);

  claims = signal<ClaimResponse[]>([]);
  loading = signal(true);
  error = signal('');
  success = signal('');
  filterStatut = signal('');
  filterType = signal('');
  searchTerm = signal('');

  // Detail panel
  selectedClaim = signal<ClaimResponse | null>(null);
  claimExpertises = signal<ExpertiseResponse[]>([]);
  detailLoading = signal(false);

  filteredClaims = computed(() => {
    let result = this.claims();
    const statut = this.filterStatut();
    const type = this.filterType();
    const search = this.searchTerm().toLowerCase();

    if (statut) {
      result = result.filter(c => c.statut === statut);
    }
    if (type) {
      result = result.filter(c => c.type === type);
    }
    if (search) {
      result = result.filter(c =>
        (c.id?.toLowerCase().includes(search)) ||
        (c.assureNom?.toLowerCase().includes(search)) ||
        (c.description?.toLowerCase().includes(search))
      );
    }
    return result;
  });

  ngOnInit(): void {
    this.loadData();
  }

  loadData(): void {
    this.loading.set(true);
    this.error.set('');
    this.claimService.getExpertDossiers().subscribe({
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

  setStatusFilter(filter: string): void {
    this.filterStatut.set(filter);
  }

  setTypeFilter(filter: string): void {
    this.filterType.set(filter);
  }

  onSearchInput(event: Event): void {
    this.searchTerm.set((event.target as HTMLInputElement).value);
  }

  resetFilters(): void {
    this.filterStatut.set('');
    this.filterType.set('');
    this.searchTerm.set('');
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
  }

  closeDetail(): void {
    this.selectedClaim.set(null);
    this.claimExpertises.set([]);
  }

  getStatutLabel(statut: string): string {
    const labels: Record<string, string> = {
      'EN_COURS': 'En cours',
      'EN_REVISION': 'En révision',
      'EXPERTISE': 'En expertise',
      'VALIDE': 'Validé',
      'REFUSE': 'Refusé',
      'CLOTURE': 'Clôturé',
      'ARCHIVE': 'Archivé'
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
      'REFUSEE': 'bg-red-100 text-red-700'
    };
    return classes[statut] || 'bg-slate-100 text-slate-600';
  }

  formatDate(dateStr: string | undefined): string {
    if (!dateStr) return '—';
    try {
      const date = new Date(dateStr);
      return date.toLocaleDateString('fr-FR', { day: 'numeric', month: 'short', year: 'numeric' });
    } catch {
      return dateStr;
    }
  }

  formatDateTime(dateStr: string | undefined): string {
    if (!dateStr) return '—';
    try {
      const date = new Date(dateStr);
      return date.toLocaleDateString('fr-FR', { day: 'numeric', month: 'short', year: 'numeric', hour: '2-digit', minute: '2-digit' });
    } catch {
      return dateStr;
    }
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

  clearSuccessAfterDelay(): void {
    setTimeout(() => this.success.set(''), 3000);
  }
}
