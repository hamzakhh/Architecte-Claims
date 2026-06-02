import { Component, OnInit, inject, signal, computed, DestroyRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { UserService, UserProfileResponse, UpdateProfileRequest, ChangePasswordRequest } from '../../core/user.service';
import { AuthService } from '../../core/auth.service';

@Component({
  selector: 'app-profil',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './profil.html'
})
export class ProfilAssure implements OnInit {
  private destroyRef = inject(DestroyRef);
  private userService = inject(UserService);
  private authService = inject(AuthService);
  private fb = inject(FormBuilder);

  profile = signal<UserProfileResponse | null>(null);
  loading = signal(true);
  error = signal<string | null>(null);
  editMode = signal(false);
  savingProfile = signal(false);
  saveProfileSuccess = signal(false);
  saveProfileError = signal('');

  showPasswordModal = signal(false);
  savingPassword = signal(false);
  passwordSuccess = signal(false);
  passwordError = signal('');

  profileForm: FormGroup;
  passwordForm: FormGroup;

  readonly userName = computed(() => this.authService.userName());
  readonly userEmail = computed(() => this.authService.userEmail());
  readonly userId = computed(() => this.authService.userId());

  constructor() {
    this.profileForm = this.fb.group({
      prenom: ['', [Validators.required, Validators.minLength(2)]],
      nom: ['', [Validators.required, Validators.minLength(2)]],
      email: ['', [Validators.required, Validators.email]],
      telephone: ['']
    });

    this.passwordForm = this.fb.group({
      currentPassword: ['', [Validators.required]],
      newPassword: ['', [Validators.required, Validators.minLength(6)]],
      confirmPassword: ['', [Validators.required]]
    }, { validators: this.passwordMatchValidator });
  }

  passwordMatchValidator(group: FormGroup): { [key: string]: boolean } | null {
    const newPassword = group.get('newPassword')?.value;
    const confirmPassword = group.get('confirmPassword')?.value;
    if (newPassword && confirmPassword && newPassword !== confirmPassword) {
      return { passwordMismatch: true };
    }
    return null;
  }

  ngOnInit(): void {
    this.loadProfile();
  }

  private loadProfile(): void {
    this.loading.set(true);
    this.error.set(null);

    this.userService.getMyProfile()
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (profile) => {
          this.profile.set(profile);
          this.profileForm.patchValue({
            prenom: profile.prenom,
            nom: profile.nom,
            email: profile.email,
            telephone: profile.telephone || ''
          });
          this.loading.set(false);
        },
        error: (err) => {
          console.error('Error loading profile:', err);
          this.error.set('Impossible de charger votre profil');
          this.loading.set(false);
        }
      });
  }

  toggleEditMode(): void {
    if (this.editMode()) {
      this.cancelEdit();
    } else {
      this.editMode.set(true);
      this.saveProfileError.set('');
      this.saveProfileSuccess.set(false);
    }
  }

  cancelEdit(): void {
    const p = this.profile();
    if (p) {
      this.profileForm.patchValue({
        prenom: p.prenom,
        nom: p.nom,
        email: p.email,
        telephone: p.telephone || ''
      });
    }
    this.editMode.set(false);
    this.saveProfileError.set('');
    this.saveProfileSuccess.set(false);
  }

  saveProfile(): void {
    if (this.profileForm.invalid) {
      Object.keys(this.profileForm.controls).forEach(key => {
        this.profileForm.get(key)?.markAsTouched();
      });
      return;
    }

    this.savingProfile.set(true);
    this.saveProfileError.set('');
    this.saveProfileSuccess.set(false);

    const request: UpdateProfileRequest = this.profileForm.value;

    this.userService.updateProfile(request)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (updatedProfile) => {
          this.profile.set(updatedProfile);
          this.savingProfile.set(false);
          this.saveProfileSuccess.set(true);
          this.editMode.set(false);
        },
        error: (err) => {
          this.savingProfile.set(false);
          const msg = err?.error?.message || err?.error || 'Erreur lors de la mise à jour';
          this.saveProfileError.set(typeof msg === 'string' ? msg : 'Erreur lors de la mise à jour');
        }
      });
  }

  openPasswordModal(): void {
    this.passwordForm.reset();
    this.passwordError.set('');
    this.passwordSuccess.set(false);
    this.showPasswordModal.set(true);
  }

  closePasswordModal(): void {
    this.showPasswordModal.set(false);
    this.passwordForm.reset();
  }

  changePassword(): void {
    if (this.passwordForm.invalid) {
      Object.keys(this.passwordForm.controls).forEach(key => {
        this.passwordForm.get(key)?.markAsTouched();
      });
      return;
    }

    this.savingPassword.set(true);
    this.passwordError.set('');
    this.passwordSuccess.set(false);

    const request: ChangePasswordRequest = this.passwordForm.value;

    this.userService.changePassword(request)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: () => {
          this.savingPassword.set(false);
          this.passwordSuccess.set(true);
          setTimeout(() => this.closePasswordModal(), 2000);
        },
        error: (err) => {
          this.savingPassword.set(false);
          const msg = err?.error?.message || err?.error || 'Erreur lors du changement de mot de passe';
          this.passwordError.set(typeof msg === 'string' ? msg : 'Erreur lors du changement de mot de passe');
        }
      });
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

  getRoleLabel(role: string): string {
    switch (role) {
      case 'ASSURE': return 'Assuré';
      case 'GESTIONNAIRE': return 'Gestionnaire';
      case 'EXPERT': return 'Expert';
      case 'ADMIN': return 'Administrateur';
      default: return role;
    }
  }

  getInitials(prenom: string, nom: string): string {
    return ((prenom?.[0] || '') + (nom?.[0] || '')).toUpperCase();
  }
}
