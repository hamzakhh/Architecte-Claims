import { Component, inject, signal, OnInit } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { AuthService } from '../../core/auth.service';
import { UserService, UserProfileResponse } from '../../core/user.service';

@Component({
  selector: 'app-profil',
  standalone: true,
  imports: [FormsModule],
  templateUrl: './profil.html'
})
export class ProfilGestionnaire implements OnInit {
  auth = inject(AuthService);
  private userService = inject(UserService);

  profile = signal<UserProfileResponse | null>(null);
  loading = signal(true);
  saving = signal(false);
  error = signal('');
  success = signal('');

  editPrenom = signal('');
  editNom = signal('');
  editEmail = signal('');
  editTelephone = signal('');

  ngOnInit(): void {
    this.loadProfile();
  }

  loadProfile(): void {
    this.loading.set(true);
    this.userService.getMyProfile().subscribe({
      next: (data) => {
        this.profile.set(data);
        this.editPrenom.set(data.prenom || '');
        this.editNom.set(data.nom || '');
        this.editEmail.set(data.email || '');
        this.editTelephone.set(data.telephone || '');
        this.loading.set(false);
      },
      error: () => {
        this.error.set('Erreur lors du chargement du profil');
        this.loading.set(false);
      }
    });
  }

  saveProfile(): void {
    this.saving.set(true);
    this.error.set('');
    this.success.set('');
    this.userService.updateProfile({
      prenom: this.editPrenom(),
      nom: this.editNom(),
      email: this.editEmail(),
      telephone: this.editTelephone()
    }).subscribe({
      next: (updated: UserProfileResponse) => {
        this.profile.set(updated);
        this.saving.set(false);
        this.success.set('Profil mis à jour avec succès');
        setTimeout(() => this.success.set(''), 3000);
      },
      error: () => {
        this.saving.set(false);
        this.error.set('Erreur lors de la mise à jour du profil');
      }
    });
  }
}
