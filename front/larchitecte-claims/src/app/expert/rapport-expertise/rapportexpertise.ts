import { Component, inject, signal, OnInit } from '@angular/core';
import { ClaimService, ClaimResponse } from '../../core/claim.service';
import { ExpertiseService, ExpertiseRequest, ExpertiseResponse } from '../../core/expertise.service';
import { AnalyseIAService, AnalyseIAResponse } from '../../core/analyse-ia.service';
import { FileService } from '../../core/file.service';
import { CommonModule } from '@angular/common';
import { ActivatedRoute } from '@angular/router';
import { FormsModule } from '@angular/forms';

@Component({
  selector: 'app-rapport-expertise',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './rapportexpertise.html'
})
export class RapportExpertise implements OnInit {
  private claimService = inject(ClaimService);
  private expertiseService = inject(ExpertiseService);
  private analyseIAService = inject(AnalyseIAService);
  private fileService = inject(FileService);
  private route = inject(ActivatedRoute);

  expertises = signal<ExpertiseResponse[]>([]);
  selectedExpertise = signal<ExpertiseResponse | null>(null);
  selectedClaim = signal<ClaimResponse | null>(null);
  analyseIA = signal<AnalyseIAResponse | null>(null);
  loading = signal(true);
  error = signal('');
  saving = signal(false);
  success = signal('');

  // Form fields
  conclusion = signal('');
  montantEstime = signal('');
  recommandation = signal('ACCEPTER');

  // File upload
  uploading = signal(false);
  uploadProgress = signal(0);
  selectedFiles = signal<File[]>([]);


  ngOnInit(): void {
    this.loadExpertises();

    this.route.queryParams.subscribe(params => {
      const expertiseId = params['expertiseId'];
      const claimId = params['claimId'];
      if (expertiseId) {
        this.selectExpertiseById(expertiseId);
      } else if (claimId) {
        this.loadClaimAndSelectExpertise(claimId);
      }
    });
  }

  loadExpertises(): void {
    this.loading.set(true);
    this.error.set('');
    this.expertiseService.getMyExpertises().subscribe({
      next: (data) => {
        this.expertises.set(data);
        this.loading.set(false);
      },
      error: () => {
        this.error.set('Erreur lors du chargement des expertises');
        this.loading.set(false);
      }
    });
  }

  selectExpertiseById(expertiseId: string): void {
    const exp = this.expertises().find(e => e.id === expertiseId);
    if (exp) {
      this.selectExpertise(exp);
    } else {
      this.expertiseService.getExpertiseById(expertiseId).subscribe({
        next: (found) => {
          this.selectExpertise(found);
          // Add to list if not already there
          if (!this.expertises().some(e => e.id === found.id)) {
            this.expertises.update(list => [...list, found]);
          }
        },
        error: () => {
          this.error.set('Expertise non trouvée');
        }
      });
    }
  }

  loadClaimAndSelectExpertise(claimId: string): void {
    this.claimService.getClaimById(claimId).subscribe({
      next: (claim) => {
        this.selectedClaim.set(claim);
        // Find existing expertise for this claim from local list
        const existing = this.expertises().find(e => e.claimId === claimId);
        if (existing) {
          this.selectExpertise(existing);
        } else {
          // Try to load expertises for this claim from API
          this.expertiseService.getExpertisesByClaim(claimId).subscribe({
            next: (expertises) => {
              if (expertises.length > 0) {
                const exp = expertises[0];
                this.selectExpertise(exp);
                if (!this.expertises().some(e => e.id === exp.id)) {
                  this.expertises.update(list => [...list, ...expertises]);
                }
              }
            },
            error: () => {}
          });
        }
      },
      error: () => {}
    });
  }

  selectExpertise(exp: ExpertiseResponse): void {
    this.selectedExpertise.set(exp);
    this.analyseIA.set(null);
    // Preserve valid falsy values (ex: "0") instead of replacing them.
    this.conclusion.set(exp.conclusion ?? '');
    this.montantEstime.set(exp.montantEstime ?? '');
    this.recommandation.set(exp.recommandation ?? 'ACCEPTER');

    // Load associated claim
    if (exp.claimId) {
      this.claimService.getClaimById(exp.claimId).subscribe({
        next: (claim) => {
          this.selectedClaim.set(claim);
          // Load AI analysis for this claim
          this.analyseIAService.getAnalyseByClaimId(claim.id).subscribe({
            next: (analyse) => this.analyseIA.set(analyse),
            error: () => {}
          });
        },
        error: () => {}
      });
    }
  }

  deselectExpertise(): void {
    this.selectedExpertise.set(null);
    this.selectedClaim.set(null);
    this.conclusion.set('');
    this.montantEstime.set('');
    this.recommandation.set('ACCEPTER');
    this.success.set('');
    this.error.set('');
  }

  onFileSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    if (input.files) {
      this.selectedFiles.set(Array.from(input.files));
    }
  }

  uploadFiles(): void {
    const exp = this.selectedExpertise();
    if (!exp || this.selectedFiles().length === 0) return;

    this.uploading.set(true);
    this.uploadProgress.set(0);
    this.error.set('');

    this.fileService.uploadFiles(this.selectedFiles()).subscribe({
      next: (filenames: string[]) => {
        this.uploading.set(false);
        this.uploadProgress.set(100);
        // Add uploaded files to expertise piecesJointes
        const currentFiles = exp.piecesJointes ? [...exp.piecesJointes] : [];
        const updatedFiles = [...currentFiles, ...filenames];
        const updatedExp = { ...exp, piecesJointes: updatedFiles };
        this.selectedExpertise.set(updatedExp);
        this.updateExpertiseInList(updatedExp);
        this.selectedFiles.set([]);
        // Auto-save draft to persist piecesJointes to backend
        const request: ExpertiseRequest = {
          claimId: updatedExp.claimId,
          conclusion: this.conclusion(),
          montantEstime: this.montantEstime(),
          recommandation: this.recommandation(),
          piecesJointes: updatedFiles
        };
        this.expertiseService.saveBrouillon(updatedExp.id, request).subscribe({
          next: (saved) => {
            this.selectedExpertise.set(saved);
            this.updateExpertiseInList(saved);
            this.success.set('Fichiers uploadés et enregistrés');
            this.clearSuccessAfterDelay();
          },
          error: () => {
            this.success.set('Fichiers uploadés (erreur lors de la sauvegarde)');
            this.clearSuccessAfterDelay();
          }
        });
      },
      error: () => {
        this.uploading.set(false);
        this.error.set('Erreur lors de l\'upload des fichiers');
      }
    });
  }

  removeFile(filename: string): void {
    const exp = this.selectedExpertise();
    if (!exp) return;
    const updatedFiles = (exp.piecesJointes || []).filter(f => f !== filename);
    const updatedExp = { ...exp, piecesJointes: updatedFiles };
    this.selectedExpertise.set(updatedExp);
    this.updateExpertiseInList(updatedExp);
    // Auto-save draft to persist removal to backend
    const request: ExpertiseRequest = {
      claimId: updatedExp.claimId,
      conclusion: this.conclusion(),
      montantEstime: this.montantEstime(),
      recommandation: this.recommandation(),
      piecesJointes: updatedFiles
    };
    this.expertiseService.saveBrouillon(updatedExp.id, request).subscribe({
      next: (saved) => {
        this.selectedExpertise.set(saved);
        this.updateExpertiseInList(saved);
        this.success.set('Document supprimé');
        this.clearSuccessAfterDelay();
      },
      error: () => {
        this.error.set('Erreur lors de la suppression du document');
      }
    });
  }

  submitRapport(): void {
    const exp = this.selectedExpertise();
    if (!exp) return;

    this.saving.set(true);
    this.error.set('');
    this.success.set('');

    const request: ExpertiseRequest = {
      claimId: exp.claimId,
      conclusion: this.conclusion(),
      montantEstime: this.montantEstime(),
      recommandation: this.recommandation(),
      piecesJointes: exp.piecesJointes || []
    };

    this.expertiseService.submitRapport(exp.id, request).subscribe({
      next: (updated) => {
        this.saving.set(false);
        this.success.set('Rapport soumis avec succès !');
        this.selectedExpertise.set(updated);
        this.updateExpertiseInList(updated);
      },
      error: () => {
        this.saving.set(false);
        this.error.set('Erreur lors de la soumission du rapport');
      }
    });
  }

  saveDraft(): void {
    const exp = this.selectedExpertise();
    if (!exp) return;

    this.saving.set(true);
    this.error.set('');
    this.success.set('');

    const request: ExpertiseRequest = {
      claimId: exp.claimId,
      conclusion: this.conclusion(),
      montantEstime: this.montantEstime(),
      recommandation: this.recommandation(),
      piecesJointes: exp.piecesJointes || []
    };

    this.expertiseService.saveBrouillon(exp.id, request).subscribe({
      next: (updated) => {
        this.saving.set(false);
        this.success.set('Brouillon enregistré');
        this.selectedExpertise.set(updated);
        this.updateExpertiseInList(updated);
        this.clearSuccessAfterDelay();
      },
      error: () => {
        this.saving.set(false);
        this.error.set('Erreur lors de l\'enregistrement');
      }
    });
  }

  getStatutLabel(statut: string): string {
    const labels: Record<string, string> = {
      'EN_ATTENTE': 'En attente',
      'EN_COURS': 'En cours',
      'SOUMISE': 'Soumise',
      'VALIDEE': 'Validée',
      'REFUSEE': 'Refusée'
    };
    return labels[statut] || statut;
  }

  getStatutClass(statut: string): string {
    const classes: Record<string, string> = {
      'EN_ATTENTE': 'bg-amber-100 text-amber-700',
      'EN_COURS': 'bg-blue-100 text-blue-700',
      'SOUMISE': 'bg-emerald-100 text-emerald-700',
      'VALIDEE': 'bg-emerald-200 text-emerald-800',
      'REFUSEE': 'bg-error-container text-on-error-container'
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

  formatDate(dateStr: string | undefined): string {
    if (!dateStr) return '';
    const date = new Date(dateStr);
    return date.toLocaleDateString('fr-FR', { day: 'numeric', month: 'short', year: 'numeric' });
  }

  updateExpertiseInList(exp: ExpertiseResponse): void {
    this.expertises.update(list => list.map(e => e.id === exp.id ? exp : e));
  }

  canEdit(): boolean {
    const exp = this.selectedExpertise();
    return exp !== null && (exp.statut === 'EN_ATTENTE' || exp.statut === 'EN_COURS');
  }

  hasRapportData(): boolean {
    const hasConclusion = this.conclusion().trim().length > 0;
    const hasMontant = this.montantEstime().trim().length > 0;
    return hasConclusion && hasMontant;
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

  getFileIcon(filename: string): string {
    const ext = filename.split('.').pop()?.toLowerCase();
    if (ext === 'pdf') return 'picture_as_pdf';
    if (['jpg', 'jpeg', 'png', 'gif', 'webp'].includes(ext || '')) return 'image';
    return 'description';
  }

  formatFileSize(bytes: number): string {
    if (bytes < 1024) return bytes + ' B';
    if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(1) + ' KB';
    return (bytes / (1024 * 1024)).toFixed(1) + ' MB';
  }

  clearSuccessAfterDelay(): void {
    setTimeout(() => this.success.set(''), 3000);
  }
}
