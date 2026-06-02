import { Component } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { AuthService, RegisterRequest } from '../../core/auth.service';

@Component({
  selector: 'app-inscription',
  standalone: true,
  imports: [FormsModule, RouterLink],
  templateUrl: './inscription.html'
})
export class Inscription {
  prenom = '';
  nom = '';
  email = '';
  telephone = '';
  password = '';
  confirmPassword = '';
  errorMessage = '';
  isLoading = false;

  constructor(private authService: AuthService) {}

  onSubmit() {
    this.errorMessage = '';

    if (!this.prenom || !this.nom || !this.email || !this.password) {
      this.errorMessage = 'Veuillez remplir tous les champs obligatoires';
      return;
    }

    if (this.password !== this.confirmPassword) {
      this.errorMessage = 'Les mots de passe ne correspondent pas';
      return;
    }

    if (this.password.length < 6) {
      this.errorMessage = 'Le mot de passe doit contenir au moins 6 caractères';
      return;
    }

    this.isLoading = true;
    const request: RegisterRequest = {
      prenom: this.prenom,
      nom: this.nom,
      email: this.email,
      password: this.password,
      telephone: this.telephone || undefined
    };

    this.authService.register(request).subscribe({
      next: () => {
        this.isLoading = false;
      },
      error: (err) => {
        this.isLoading = false;
        this.errorMessage = err.error?.error || 'Erreur lors de l\'inscription';
      }
    });
  }
}
