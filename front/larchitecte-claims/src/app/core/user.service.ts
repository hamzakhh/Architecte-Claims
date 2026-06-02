import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface UserProfileResponse {
  id: string;
  prenom: string;
  nom: string;
  fullName: string;
  email: string;
  telephone: string;
  role: string;
  specialite?: string;
  zoneIntervention?: string;
  notePerformance?: number;
  chargeMax?: number;
  certifications?: string[];
  enabled: boolean;
  createdAt: string;
  updatedAt: string;
}

export interface UpdateProfileRequest {
  prenom: string;
  nom: string;
  email: string;
  telephone: string;
}

export interface ChangePasswordRequest {
  currentPassword: string;
  newPassword: string;
  confirmPassword: string;
}

@Injectable({ providedIn: 'root' })
export class UserService {
  private readonly API_URL = '/api/users';
  // private readonly API_URL = '/api/users';

  constructor(private http: HttpClient) {}

  getMyProfile(): Observable<UserProfileResponse> {
    return this.http.get<UserProfileResponse>(`${this.API_URL}/me`);
  }

  updateProfile(request: UpdateProfileRequest): Observable<UserProfileResponse> {
    return this.http.put<UserProfileResponse>(`${this.API_URL}/me`, request);
  }

  changePassword(request: ChangePasswordRequest): Observable<{ message: string }> {
    return this.http.put<{ message: string }>(`${this.API_URL}/me/password`, request);
  }
}
