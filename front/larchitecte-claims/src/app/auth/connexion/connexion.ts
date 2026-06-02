import { Component } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { AuthService, LoginRequest } from '../../core/auth.service';

@Component({
  selector: 'app-connexion',
  standalone: true,
  imports: [FormsModule, RouterLink],
  templateUrl: './connexion.html'
})
export class Connexion {
  email = '';
  password = '';
  errorMessage = '';
  isLoading = false;
  showPassword = false;

  constructor(private authService: AuthService) {}

  togglePasswordVisibility() {
    this.showPassword = !this.showPassword;
  }

  onSubmit() {
    this.errorMessage = '';
    if (!this.email || !this.password) {
      this.errorMessage = 'Veuillez remplir tous les champs';
      return;
    }

    this.isLoading = true;
    const request: LoginRequest = { email: this.email, password: this.password };

    this.authService.login(request).subscribe({
      next: () => {
        this.isLoading = false;
      },
      error: (err) => {
        this.isLoading = false;
        this.errorMessage = err.error?.error || 'Email ou mot de passe incorrect';
      }
    });
  }
}
