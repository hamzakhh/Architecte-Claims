import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { UserProfileResponse } from './user.service';

export interface AdminDashboardStatsResponse {
  totalUtilisateurs: number;
  utilisateursActifs: number;
  totalAssures: number;
  totalExperts: number;
  totalGestionnaires: number;
  totalAdmins: number;
  totalSinistres: number;
  sinistresEnCours: number;
  sinistresClotures: number;
  sinistresCeMois: number;
  expertisesEnCours: number;
  expertsDisponibles: number;
}

export interface RegisterRequest {
  prenom: string;
  nom: string;
  email: string;
  password: string;
  telephone?: string;
  role?: string;
  specialite?: string;
  zoneIntervention?: string;
  notePerformance?: number;
  chargeMax?: number;
}

// Analytics
export interface TypeStat {
  type: string;
  label: string;
  count: number;
  percentage: number;
}

export interface MonthlyStat {
  mois: string;
  label: string;
  count: number;
}

export interface RegionStat {
  region: string;
  sinistres: number;
  delaiMoyenJours: number;
  satisfaction: number;
}

export interface FinancialIndicator {
  label: string;
  description: string;
  value: string;
  trend: string;
}

export interface AnalyticsStatsResponse {
  totalSinistresDeclares: number;
  totalIndemnisations: number;
  delaiMoyenTraitementJours: number;
  tauxSatisfaction: number;
  sinistresParType: TypeStat[];
  tendanceMensuelle: MonthlyStat[];
  performanceParRegion: RegionStat[];
  indicateursFinanciers: FinancialIndicator[];
}

// Audit Log
export interface AuditLogResponse {
  id: string;
  action: string;
  utilisateur: string;
  role: string;
  cible: string;
  details: string;
  timestamp: string;
}

// Workload
export interface WorkloadResponse {
  userId: string;
  fullName: string;
  role: string;
  dossiersActifs: number;
  dossiersTotal: number;
  chargePourcentage: number;
  chargeMax: number;
}


@Injectable({ providedIn: 'root' })
export class AdminService {
 private readonly API_URL = '/api/users';
  private readonly ADMIN_API_URL = '/api/admin';
  // private readonly API_URL = '/api/users';
  // private readonly ADMIN_API_URL = '/api/admin';

  constructor(private http: HttpClient) {}

  getAdminStats(): Observable<AdminDashboardStatsResponse> {
    return this.http.get<AdminDashboardStatsResponse>(`${this.API_URL}/admin-stats`);
  }

  getAllUsers(): Observable<UserProfileResponse[]> {
    return this.http.get<UserProfileResponse[]>(this.API_URL);
  }

  getUsersByRole(role: string): Observable<UserProfileResponse[]> {
    return this.http.get<UserProfileResponse[]>(`${this.API_URL}/role/${role}`);
  }

  createUser(request: RegisterRequest): Observable<UserProfileResponse> {
    return this.http.post<UserProfileResponse>(this.API_URL, request);
  }

  updateUserRole(userId: string, newRole: string): Observable<UserProfileResponse> {
    return this.http.patch<UserProfileResponse>(`${this.API_URL}/${userId}/role`, newRole);
  }

  deleteUser(userId: string): Observable<{ message: string }> {
    return this.http.delete<{ message: string }>(`${this.API_URL}/${userId}`);
  }

  toggleUserStatus(userId: string): Observable<UserProfileResponse> {
    return this.http.patch<UserProfileResponse>(`${this.API_URL}/${userId}/toggle-status`, {});
  }

  searchUsers(term: string): Observable<UserProfileResponse[]> {
    return this.http.get<UserProfileResponse[]>(`${this.API_URL}/search`, { params: { term } });
  }

  // Analytics
  getAnalyticsStats(): Observable<AnalyticsStatsResponse> {
    return this.http.get<AnalyticsStatsResponse>(`${this.ADMIN_API_URL}/analytics`);
  }

  // Audit Logs
  getAuditLogs(): Observable<AuditLogResponse[]> {
    return this.http.get<AuditLogResponse[]>(`${this.ADMIN_API_URL}/audit-logs`);
  }

  // Workload
  getWorkload(): Observable<WorkloadResponse[]> {
    return this.http.get<WorkloadResponse[]>(`${this.ADMIN_API_URL}/workload`);
  }

}
