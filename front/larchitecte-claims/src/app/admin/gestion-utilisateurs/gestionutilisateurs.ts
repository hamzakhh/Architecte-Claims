import { Component, inject, signal, OnInit, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { AdminService, RegisterRequest } from '../../core/admin.service';
import { UserProfileResponse } from '../../core/user.service';
import { getSpecialiteLabel } from '../../shared/gestionnaire-helpers';

@Component({
  selector: 'app-gestion-utilisateurs',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './gestionutilisateurs.html'
})
export class GestionUtilisateurs implements OnInit {
  private adminService = inject(AdminService);

  users = signal<UserProfileResponse[]>([]);
  loading = signal(true);
  error = signal('');
  successMessage = signal('');

  // Filters
  searchTerm = signal('');
  roleFilter = signal('ALL');
  statusFilter = signal('ALL');

  filteredUsers = computed(() => {
    let result = this.users();
    const term = this.searchTerm().toLowerCase();
    if (term) {
      result = result.filter(u =>
        u.fullName.toLowerCase().includes(term) ||
        u.email.toLowerCase().includes(term)
      );
    }
    const role = this.roleFilter();
    if (role !== 'ALL') result = result.filter(u => u.role === role);
    const status = this.statusFilter();
    if (status === 'ACTIVE') result = result.filter(u => u.enabled);
    if (status === 'INACTIVE') result = result.filter(u => !u.enabled);
    return result;
  });

  // Stats
  totalUsers = computed(() => this.users().length);
  totalAssures = computed(() => this.users().filter(u => u.role === 'ASSURE').length);
  totalExperts = computed(() => this.users().filter(u => u.role === 'EXPERT').length);
  totalGestionnaires = computed(() => this.users().filter(u => u.role === 'GESTIONNAIRE').length);

  // MFA tracking (local)
  mfaEnabled = signal<Set<string>>(new Set());

  // Modal states
  showCreateModal = signal(false);
  showEditRoleModal = signal(false);
  showDeleteConfirmModal = signal(false);
  showDetailModal = signal(false);

  // Form models
  newUser: RegisterRequest = { prenom: '', nom: '', email: '', password: '', role: 'ASSURE' };
  selectedUser = signal<UserProfileResponse | null>(null);
  editRole = signal('');
  createError = signal('');

  ngOnInit(): void {
    this.loadData();
  }

  loadData(): void {
    this.loading.set(true);
    this.adminService.getAllUsers().subscribe({
      next: (data) => {
        this.users.set(data);
        this.loading.set(false);
      },
      error: () => {
        this.error.set('Erreur lors du chargement des utilisateurs');
        this.loading.set(false);
      }
    });
  }

  onSearchInput(event: Event): void {
    this.searchTerm.set((event.target as HTMLInputElement).value);
  }

  setRoleFilter(role: string): void {
    this.roleFilter.set(role);
  }

  setStatusFilter(status: string): void {
    this.statusFilter.set(status);
  }

  // Create user
  openCreateModal(): void {
    this.newUser = { prenom: '', nom: '', email: '', password: '', telephone: '', role: 'ASSURE' };
    this.createError.set('');
    this.showCreateModal.set(true);
  }

  closeCreateModal(): void {
    this.showCreateModal.set(false);
    this.createError.set('');
  }

  confirmCreate(): void {
    if (!this.newUser.prenom || !this.newUser.nom || !this.newUser.email || !this.newUser.password) {
      this.createError.set('Tous les champs obligatoires doivent être remplis');
      return;
    }
    this.adminService.createUser(this.newUser).subscribe({
      next: () => {
        this.showCreateModal.set(false);
        this.successMessage.set('Utilisateur créé avec succès');
        this.loadData();
        setTimeout(() => this.successMessage.set(''), 3000);
      },
      error: (err) => {
        this.createError.set(err.error?.message || 'Erreur lors de la création');
      }
    });
  }

  // Edit role
  openEditRoleModal(user: UserProfileResponse): void {
    this.selectedUser.set(user);
    this.editRole.set(user.role);
    this.showEditRoleModal.set(true);
  }

  closeEditRoleModal(): void {
    this.showEditRoleModal.set(false);
    this.selectedUser.set(null);
  }

  confirmEditRole(): void {
    const user = this.selectedUser();
    if (!user) return;
    this.adminService.updateUserRole(user.id, this.editRole()).subscribe({
      next: (updated) => {
        this.users.update(list => list.map(u => u.id === updated.id ? updated : u));
        this.showEditRoleModal.set(false);
        this.selectedUser.set(null);
        this.successMessage.set('Rôle mis à jour avec succès');
        setTimeout(() => this.successMessage.set(''), 3000);
      },
      error: () => {
        this.error.set('Erreur lors de la mise à jour du rôle');
      }
    });
  }

  // Toggle status
  toggleStatus(user: UserProfileResponse): void {
    this.adminService.toggleUserStatus(user.id).subscribe({
      next: (updated) => {
        this.users.update(list => list.map(u => u.id === updated.id ? updated : u));
        this.successMessage.set(updated.enabled ? 'Utilisateur activé' : 'Utilisateur désactivé');
        setTimeout(() => this.successMessage.set(''), 3000);
      },
      error: () => {
        this.error.set('Erreur lors du changement de statut');
      }
    });
  }

  // Delete user
  openDeleteConfirm(user: UserProfileResponse): void {
    this.selectedUser.set(user);
    this.showDeleteConfirmModal.set(true);
  }

  closeDeleteConfirm(): void {
    this.showDeleteConfirmModal.set(false);
    this.selectedUser.set(null);
  }

  confirmDelete(): void {
    const user = this.selectedUser();
    if (!user) return;
    this.adminService.deleteUser(user.id).subscribe({
      next: () => {
        this.users.update(list => list.filter(u => u.id !== user.id));
        this.showDeleteConfirmModal.set(false);
        this.selectedUser.set(null);
        this.successMessage.set('Utilisateur supprimé avec succès');
        setTimeout(() => this.successMessage.set(''), 3000);
      },
      error: () => {
        this.error.set('Erreur lors de la suppression');
      }
    });
  }

  // MFA toggle
  toggleMfa(user: UserProfileResponse): void {
    this.mfaEnabled.update(set => {
      const newSet = new Set(set);
      if (newSet.has(user.id)) newSet.delete(user.id);
      else newSet.add(user.id);
      return newSet;
    });
    const isEnabled = this.mfaEnabled().has(user.id);
    this.successMessage.set(isEnabled ? `MFA activé pour ${user.fullName}` : `MFA désactivé pour ${user.fullName}`);
    setTimeout(() => this.successMessage.set(''), 3000);
  }

  isMfaEnabled(userId: string): boolean {
    return this.mfaEnabled().has(userId);
  }

  // View detail
  openDetailModal(user: UserProfileResponse): void {
    this.selectedUser.set(user);
    this.showDetailModal.set(true);
  }

  closeDetailModal(): void {
    this.showDetailModal.set(false);
    this.selectedUser.set(null);
  }

  getRoleLabel(role: string): string {
    const labels: Record<string, string> = {
      'ASSURE': 'Assuré',
      'EXPERT': 'Expert',
      'GESTIONNAIRE': 'Gestionnaire',
      'ADMIN': 'Admin'
    };
    return labels[role] || role;
  }

  getRoleClass(role: string): string {
    const classes: Record<string, string> = {
      'ASSURE': 'bg-primary/10 text-primary',
      'EXPERT': 'bg-amber-100 text-amber-700',
      'GESTIONNAIRE': 'bg-emerald-100 text-emerald-700',
      'ADMIN': 'bg-blue-100 text-blue-700'
    };
    return classes[role] || 'bg-slate-100 text-slate-600';
  }

  getRoleIcon(role: string): string {
    const icons: Record<string, string> = {
      'ASSURE': 'person',
      'EXPERT': 'engineering',
      'GESTIONNAIRE': 'manage_accounts',
      'ADMIN': 'admin_panel_settings'
    };
    return icons[role] || 'person';
  }

  getSpecialiteLabel = getSpecialiteLabel;

  formatDate(dateStr: string): string {
    if (!dateStr) return '—';
    try {
      const date = new Date(dateStr);
      return date.toLocaleDateString('fr-FR', { day: 'numeric', month: 'short', year: 'numeric' });
    } catch {
      return dateStr;
    }
  }
}
