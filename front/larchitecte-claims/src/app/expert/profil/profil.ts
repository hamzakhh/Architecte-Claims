import { Component, inject, signal, OnInit } from '@angular/core';
import { UserService, UserProfileResponse, UpdateProfileRequest, ChangePasswordRequest } from '../../core/user.service';
import { AuthService } from '../../core/auth.service';
import { ExpertiseService, ExpertiseResponse } from '../../core/expertise.service';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';

@Component({
  selector: 'app-profil',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './profil.html'
})
export class ProfilExpert implements OnInit {
  private userService = inject(UserService);
  private authService = inject(AuthService);
  private expertiseService = inject(ExpertiseService);

  profile = signal<UserProfileResponse | null>(null);
  loading = signal(true);
  error = signal('');
  success = signal('');
  editing = signal(false);

  // Edit form
  editPrenom = signal('');
  editNom = signal('');
  editEmail = signal('');
  editTelephone = signal('');

  // Password change
  showPasswordModal = signal(false);
  currentPassword = signal('');
  newPassword = signal('');
  confirmPassword = signal('');
  passwordError = signal('');
  passwordSaving = signal(false);

  // Stats
  totalExpertises = signal(0);
  expertisesCompletees = signal(0);
  expertisesEnCours = signal(0);

  saving = signal(false);

  ngOnInit(): void {
    this.loadProfile();
  }

  loadProfile(): void {
    this.loading.set(true);
    this.userService.getMyProfile().subscribe({
      next: (data) => {
        this.profile.set(data);
        this.editPrenom.set(data.prenom);
        this.editNom.set(data.nom);
        this.editEmail.set(data.email);
        this.editTelephone.set(data.telephone || '');
        this.loading.set(false);
      },
      error: () => {
        this.error.set('Erreur lors du chargement du profil');
        this.loading.set(false);
      }
    });
    this.expertiseService.getMyExpertises().subscribe({
      next: (data) => {
        this.totalExpertises.set(data.length);
        this.expertisesCompletees.set(data.filter(e => e.statut === 'VALIDEE' || e.statut === 'SOUMISE').length);
        this.expertisesEnCours.set(data.filter(e => e.statut === 'EN_ATTENTE' || e.statut === 'EN_COURS').length);
      },
      error: () => {}
    });
  }

  startEditing(): void {
    this.editing.set(true);
    this.error.set('');
    this.success.set('');
  }

  cancelEditing(): void {
    const p = this.profile();
    if (p) {
      this.editPrenom.set(p.prenom);
      this.editNom.set(p.nom);
      this.editEmail.set(p.email);
      this.editTelephone.set(p.telephone || '');
    }
    this.editing.set(false);
  }

  saveProfile(): void {
    this.saving.set(true);
    this.error.set('');
    const request: UpdateProfileRequest = {
      prenom: this.editPrenom(),
      nom: this.editNom(),
      email: this.editEmail(),
      telephone: this.editTelephone()
    };
    this.userService.updateProfile(request).subscribe({
      next: (updated) => {
        this.profile.set(updated);
        this.editing.set(false);
        this.saving.set(false);
        this.success.set('Profil mis à jour avec succès');
        this.clearSuccessAfterDelay();
      },
      error: () => {
        this.error.set('Erreur lors de la mise à jour du profil');
        this.saving.set(false);
      }
    });
  }

  openPasswordModal(): void {
    this.currentPassword.set('');
    this.newPassword.set('');
    this.confirmPassword.set('');
    this.passwordError.set('');
    this.showPasswordModal.set(true);
  }

  closePasswordModal(): void {
    this.showPasswordModal.set(false);
  }

  changePassword(): void {
    if (this.newPassword() !== this.confirmPassword()) {
      this.passwordError.set('Les mots de passe ne correspondent pas');
      return;
    }
    if (this.newPassword().length < 6) {
      this.passwordError.set('Le mot de passe doit contenir au moins 6 caractères');
      return;
    }
    this.passwordSaving.set(true);
    this.passwordError.set('');
    const request: ChangePasswordRequest = {
      currentPassword: this.currentPassword(),
      newPassword: this.newPassword(),
      confirmPassword: this.confirmPassword()
    };
    this.userService.changePassword(request).subscribe({
      next: () => {
        this.passwordSaving.set(false);
        this.showPasswordModal.set(false);
        this.success.set('Mot de passe modifié avec succès');
        this.clearSuccessAfterDelay();
      },
      error: () => {
        this.passwordSaving.set(false);
        this.passwordError.set('Erreur lors du changement de mot de passe');
      }
    });
  }

  getRoleLabel(role: string): string {
    const labels: Record<string, string> = {
      'EXPERT': 'Expert',
      'GESTIONNAIRE': 'Gestionnaire',
      'ASSURE': 'Assuré',
      'ADMIN': 'Administrateur'
    };
    return labels[role] || role;
  }

  getCompletionRate(): string {
    const total = this.totalExpertises();
    if (total === 0) return '0%';
    const completed = this.expertisesCompletees();
    return Math.round((completed / total) * 100) + '%';
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

  clearSuccessAfterDelay(): void {
    setTimeout(() => this.success.set(''), 3000);
  }
}
