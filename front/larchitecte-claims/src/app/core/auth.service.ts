import { Injectable, signal, computed } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Router } from '@angular/router';
import { Observable, tap, catchError, throwError } from 'rxjs';

export interface AuthResponse {
  token: string;
  email: string;
  prenom: string;
  nom: string;
  fullName: string;
  role: string;
  userId: string;
}

export interface LoginRequest {
  email: string;
  password: string;
}

export interface RegisterRequest {
  prenom: string;
  nom: string;
  email: string;
  password: string;
  telephone?: string;
}


@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly API_URL = '/api/auth';
  // private readonly API_URL = '/api/auth';
  private readonly TOKEN_KEY = 'larchitecte_token';
  private readonly USER_KEY = 'larchitecte_user';

  private currentUser = signal<AuthResponse | null>(this.loadUserFromStorage());
  readonly isAuthenticated = computed(() => !!this.currentUser());
  readonly userRole = computed(() => this.currentUser()?.role ?? null);
  readonly userName = computed(() => this.currentUser()?.fullName ?? '');
  readonly userEmail = computed(() => this.currentUser()?.email ?? '');
  readonly userId = computed(() => this.currentUser()?.userId ?? '');

  constructor(private http: HttpClient, private router: Router) {}

  login(request: LoginRequest): Observable<AuthResponse> {
    return this.http.post<AuthResponse>(`${this.API_URL}/login`, request).pipe(
      tap(response => this.handleAuthSuccess(response)),
      catchError(error => throwError(() => error))
    );
  }

  register(request: RegisterRequest): Observable<AuthResponse> {
    return this.http.post<AuthResponse>(`${this.API_URL}/register`, request).pipe(
      tap(response => this.handleAuthSuccess(response)),
      catchError(error => throwError(() => error))
    );
  }

  logout(): void {
    localStorage.removeItem(this.TOKEN_KEY);
    localStorage.removeItem(this.USER_KEY);
    this.currentUser.set(null);
    this.router.navigate(['/auth/connexion']);
  }

  getToken(): string | null {
    return localStorage.getItem(this.TOKEN_KEY);
  }

  getUser(): AuthResponse | null {
    return this.currentUser();
  }

  hasRole(role: string): boolean {
    return this.userRole() === role;
  }

  hasAnyRole(roles: string[]): boolean {
    const currentRole = this.userRole();
    return currentRole !== null && roles.includes(currentRole);
  }

  private handleAuthSuccess(response: AuthResponse): void {
    localStorage.setItem(this.TOKEN_KEY, response.token);
    localStorage.setItem(this.USER_KEY, JSON.stringify(response));
    this.currentUser.set(response);

    const roleRouteMap: Record<string, string> = {
      ASSURE: '/assure',
      GESTIONNAIRE: '/gestionnaire',
      EXPERT: '/expert',
      ADMIN: '/admin'
    };

    const route = roleRouteMap[response.role] ?? '/assure';
    this.router.navigate([route]);
  }

  private loadUserFromStorage(): AuthResponse | null {
    const stored = localStorage.getItem(this.USER_KEY);
    if (stored) {
      try {
        return JSON.parse(stored);
      } catch {
        return null;
      }
    }
    return null;
  }
}
