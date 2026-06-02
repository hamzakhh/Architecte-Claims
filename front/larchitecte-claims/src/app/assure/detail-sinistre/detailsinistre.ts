import { Component, OnInit, inject, signal, DestroyRef } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { CommonModule } from '@angular/common';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { ClaimService, ClaimResponse, ClaimHistoryEntry } from '../../core/claim.service';
import { AnalyseIAService, AnalyseIAResponse } from '../../core/analyse-ia.service';
import { FileService } from '../../core/file.service';

@Component({
  selector: 'app-detail-sinistre',
  standalone: true,
  imports: [
    CommonModule
  ],
  templateUrl: './detailsinistre.html'
})
export class DetailSinistre implements OnInit {
  private destroyRef = inject(DestroyRef);
  private route = inject(ActivatedRoute);
  private router = inject(Router);
  private claimService = inject(ClaimService);
  private analyseIAService = inject(AnalyseIAService);
  private fileService = inject(FileService);
  
  claim = signal<ClaimResponse | null>(null);
  history = signal<ClaimHistoryEntry[]>([]);
  analyseIA = signal<AnalyseIAResponse | null>(null);
  loading = signal(true);
  actionLoading = signal(false);
  error = signal<string | null>(null);

  ngOnInit(): void {
    const claimId = this.route.snapshot.paramMap.get('id');
    if (!claimId) {
      this.error.set('Identifiant de sinistre non trouvé');
      this.loading.set(false);
      return;
    }

    this.loadClaimDetails(claimId);
  }

  private loadClaimDetails(id: string): void {
    this.loading.set(true);
    this.error.set(null);

    this.claimService.getClaimById(id)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (claim) => {
          this.claim.set(claim);
          this.loading.set(false);
          this.loadHistory(claim.id);
          this.loadAnalyseIA(claim.id);
        },
        error: (err) => {
          console.error('Error loading claim details:', err);
          this.error.set('Impossible de charger les détails du sinistre');
          this.loading.set(false);
        }
      });
  }

  private loadHistory(claimId: string): void {
    this.claimService.getClaimHistory(claimId)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (data) => this.history.set(data),
        error: (err) => console.error('Erreur chargement historique', err)
      });
  }

  private loadAnalyseIA(claimId: string): void {
    this.analyseIAService.getAnalyseByClaimId(claimId)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (data) => this.analyseIA.set(data),
        error: () => this.analyseIA.set(null)
      });
  }

  accepterIndemnisation(): void {
    const claim = this.claim();
    if (!claim) return;
    this.actionLoading.set(true);
    this.claimService.accepterIndemnisation(claim.id)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: () => {
          this.actionLoading.set(false);
          this.loadClaimDetails(claim.id);
        },
        error: () => {
          this.error.set('Erreur lors de l\'acceptation');
          this.actionLoading.set(false);
        }
      });
  }

  refuserIndemnisation(): void {
    const claim = this.claim();
    if (!claim) return;
    this.actionLoading.set(true);
    this.claimService.refuserIndemnisation(claim.id, 'Refus par l\'assuré')
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: () => {
          this.actionLoading.set(false);
          this.loadClaimDetails(claim.id);
        },
        error: () => {
          this.error.set('Erreur lors du refus');
          this.actionLoading.set(false);
        }
      });
  }

  getCategoryLabel(categorie: string): string {
    switch (categorie) {
      case 'accident': return 'Accident';
      case 'incendie': return 'Incendie';
      case 'vol': return 'Vol';
      case 'degat_eaux': return 'Dégât des eaux';
      case 'catastrophe': return 'Catastrophe naturelle';
      default: return 'Autre';
    }
  }

  getActionIcon(action: string): string {
    switch (action) {
      case 'CREATION': return 'add_circle';
      case 'CHANGEMENT_STATUT': return 'swap_horiz';
      case 'PRISE_EN_CHARGE': return 'how_to_reg';
      case 'ASSIGNATION_GESTIONNAIRE': return 'person_add';
      case 'ASSIGNATION_EXPERT': return 'engineering';
      default: return 'info';
    }
  }

  getStatusLabel(status: string): string {
    switch (status) {
      case 'EN_COURS': return 'En cours';
      case 'EN_REVISION': return 'En révision';
      case 'EXPERTISE': return 'Expertise';
      case 'VALIDE': return 'Validé';
      case 'REFUSE': return 'Refusé';
      case 'CLOTURE': return 'Clôturé';
      default: return status;
    }
  }

  getStatusColor(status: string): string {
    switch (status) {
      case 'EN_COURS': return 'bg-amber-100 text-amber-700';
      case 'EN_REVISION': return 'bg-blue-100 text-blue-700';
      case 'EXPERTISE': return 'bg-purple-100 text-purple-700';
      case 'VALIDE': return 'bg-emerald-100 text-emerald-700';
      case 'REFUSE': return 'bg-red-100 text-red-700';
      case 'CLOTURE': return 'bg-slate-100 text-slate-600';
      default: return 'bg-slate-100 text-slate-600';
    }
  }

  formatDate(dateString: string): string {
    if (!dateString) return 'N/A';
    const date = new Date(dateString);
    return date.toLocaleDateString('fr-FR', {
      day: 'numeric',
      month: 'long',
      year: 'numeric'
    });
  }

  formatDateTime(dateString: string): string {
    if (!dateString) return 'N/A';
    const date = new Date(dateString);
    return date.toLocaleDateString('fr-FR', {
      day: 'numeric',
      month: 'long',
      year: 'numeric',
      hour: '2-digit',
      minute: '2-digit'
    });
  }

  getFileIcon(fileName: string): string {
    const extension = fileName?.split('.').pop()?.toLowerCase();
    switch (extension) {
      case 'pdf':
        return 'picture_as_pdf';
      case 'jpg':
      case 'jpeg':
      case 'png':
      case 'gif':
        return 'image';
      case 'doc':
      case 'docx':
        return 'description';
      case 'xls':
      case 'xlsx':
        return 'table_chart';
      case 'zip':
      case 'rar':
        return 'folder_zip';
      default:
        return 'insert_drive_file';
    }
  }

  getFileColor(fileName: string): string {
    const extension = fileName?.split('.').pop()?.toLowerCase();
    switch (extension) {
      case 'pdf':
        return 'text-red-600';
      case 'jpg':
      case 'jpeg':
      case 'png':
      case 'gif':
        return 'text-blue-600';
      case 'doc':
      case 'docx':
        return 'text-blue-500';
      case 'xls':
      case 'xlsx':
        return 'text-green-600';
      case 'zip':
      case 'rar':
        return 'text-orange-600';
      default:
        return 'text-gray-600';
    }
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

  goBack(): void {
    this.router.navigate(['/assure/mes-sinistres']);
  }

  getTimelineStatus(status: string): { completed: boolean; current: boolean } {
    const claimStatus = this.claim()?.statut;
    
    switch (status) {
      case 'Déclaration reçue':
        return { completed: true, current: false };
      case 'Expert assigné':
        return { 
          completed: claimStatus === 'EXPERTISE' || claimStatus === 'VALIDE' || claimStatus === 'CLOTURE' || claimStatus === 'REFUSE',
          current: claimStatus === 'EN_REVISION'
        };
      case 'Expertise en cours':
        return { 
          completed: claimStatus === 'VALIDE' || claimStatus === 'CLOTURE' || claimStatus === 'REFUSE',
          current: claimStatus === 'EXPERTISE'
        };
      case 'Indemnisation':
        return { 
          completed: claimStatus === 'CLOTURE',
          current: claimStatus === 'VALIDE'
        };
      default:
        return { completed: false, current: false };
    }
  }
}
