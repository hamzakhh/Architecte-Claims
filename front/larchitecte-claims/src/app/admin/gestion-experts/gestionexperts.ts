import { Component, inject, signal, OnInit, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { AdminService, RegisterRequest } from '../../core/admin.service';
import { ClaimService, ExpertProfileResponse, ClaimResponse } from '../../core/claim.service';
import { UserProfileResponse } from '../../core/user.service';
import { getStatutLabel, getStatutClass, getTypeLabel, formatDate, getSpecialiteLabel } from '../../shared/gestionnaire-helpers';

@Component({
  selector: 'app-gestion-experts',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './gestionexperts.html'
})
export class GestionExperts implements OnInit {
  private adminService = inject(AdminService);
  private claimService = inject(ClaimService);

  experts = signal<UserProfileResponse[]>([]);
  claims = signal<ClaimResponse[]>([]);
  loading = signal(true);
  error = signal('');
  successMessage = signal('');

  // Filters
  searchTerm = signal('');
  statusFilter = signal('ALL');

  filteredExperts = computed(() => {
    let result = this.experts();
    const term = this.searchTerm().toLowerCase();
    if (term) {
      result = result.filter(e =>
        e.fullName.toLowerCase().includes(term) ||
        e.email.toLowerCase().includes(term) ||
        (e.telephone || '').includes(term)
      );
    }
    const status = this.statusFilter();
    if (status === 'ACTIVE') result = result.filter(e => e.enabled);
    if (status === 'INACTIVE') result = result.filter(e => !e.enabled);
    return result;
  });

  // Stats
  totalExperts = computed(() => this.experts().length);
  availableCount = computed(() => this.experts().filter(e => e.enabled).length);
  inactiveCount = computed(() => this.experts().filter(e => !e.enabled).length);

  // Modal states
  showCreateModal = signal(false);
  showDetailModal = signal(false);
  showDeleteConfirmModal = signal(false);

  // Form models
  newExpert: RegisterRequest = { prenom: '', nom: '', email: '', password: '', role: 'EXPERT' };
  selectedExpert = signal<UserProfileResponse | null>(null);
  expertDossiers = computed(() => {
    const expert = this.selectedExpert();
    if (!expert) return [];
    return this.claims().filter(c => c.expertId === expert.id);
  });
  expertActiveDossiers = computed(() =>
    this.expertDossiers().filter(c => c.statut !== 'CLOTURE' && c.statut !== 'REFUSE').length
  );
  expertCloturedDossiers = computed(() =>
    this.expertDossiers().filter(c => c.statut === 'CLOTURE').length
  );
  createError = signal('');

  ngOnInit(): void {
    this.loadData();
  }

  loadData(): void {
    this.loading.set(true);
    this.adminService.getUsersByRole('EXPERT').subscribe({
      next: (data) => {
        this.experts.set(data);
        this.loading.set(false);
      },
      error: () => {
        this.error.set('Erreur lors du chargement des experts');
        this.loading.set(false);
      }
    });
    this.claimService.getAllClaims().subscribe({
      next: (data) => this.claims.set(data),
      error: () => {}
    });
  }

  onSearchInput(event: Event): void {
    this.searchTerm.set((event.target as HTMLInputElement).value);
  }

  setStatusFilter(status: string): void {
    this.statusFilter.set(status);
  }

  getExpertDossierCount(expertId: string): number {
    return this.claims().filter(c => c.expertId === expertId && c.statut !== 'CLOTURE' && c.statut !== 'REFUSE').length;
  }

  // Create expert
  openCreateModal(): void {
    this.newExpert = { prenom: '', nom: '', email: '', password: '', telephone: '', role: 'EXPERT', specialite: '', zoneIntervention: '', notePerformance: undefined, chargeMax: undefined };
    this.createError.set('');
    this.showCreateModal.set(true);
  }

  closeCreateModal(): void {
    this.showCreateModal.set(false);
    this.createError.set('');
  }

  confirmCreate(): void {
    if (!this.newExpert.prenom || !this.newExpert.nom || !this.newExpert.email || !this.newExpert.password) {
      this.createError.set('Tous les champs obligatoires doivent être remplis');
      return;
    }
    this.adminService.createUser(this.newExpert).subscribe({
      next: () => {
        this.showCreateModal.set(false);
        this.successMessage.set('Expert créé avec succès');
        this.loadData();
        setTimeout(() => this.successMessage.set(''), 3000);
      },
      error: (err) => {
        this.createError.set(err.error?.message || 'Erreur lors de la création');
      }
    });
  }

  // Toggle status
  toggleExpertStatus(expert: UserProfileResponse): void {
    this.adminService.toggleUserStatus(expert.id).subscribe({
      next: (updated) => {
        this.experts.update(list => list.map(e => e.id === updated.id ? updated : e));
        if (this.selectedExpert()?.id === updated.id) {
          this.selectedExpert.set(updated);
        }
        this.successMessage.set(updated.enabled ? 'Expert activé' : 'Expert désactivé');
        setTimeout(() => this.successMessage.set(''), 3000);
      },
      error: () => {
        this.error.set('Erreur lors du changement de statut');
      }
    });
  }

  // View detail
  openDetailModal(expert: UserProfileResponse): void {
    this.selectedExpert.set(expert);
    this.showDetailModal.set(true);
  }

  closeDetailModal(): void {
    this.showDetailModal.set(false);
    this.selectedExpert.set(null);
  }

  // Delete expert
  openDeleteConfirm(expert: UserProfileResponse): void {
    this.selectedExpert.set(expert);
    this.showDeleteConfirmModal.set(true);
  }

  closeDeleteConfirm(): void {
    this.showDeleteConfirmModal.set(false);
    this.selectedExpert.set(null);
  }

  confirmDelete(): void {
    const expert = this.selectedExpert();
    if (!expert) return;
    this.adminService.deleteUser(expert.id).subscribe({
      next: () => {
        this.experts.update(list => list.filter(e => e.id !== expert.id));
        this.showDeleteConfirmModal.set(false);
        this.selectedExpert.set(null);
        this.successMessage.set('Expert supprimé avec succès');
        setTimeout(() => this.successMessage.set(''), 3000);
      },
      error: () => {
        this.error.set('Erreur lors de la suppression');
      }
    });
  }

  getStatutLabel = getStatutLabel;
  getStatutClass = getStatutClass;
  getTypeLabel = getTypeLabel;
  formatDate = formatDate;
  getSpecialiteLabel = getSpecialiteLabel;
}
