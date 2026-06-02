import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface FraudAlertRequest {
  claimId: string;
  motif: string;
  description?: string;
  niveauRisque: string;
  piecesJustificatives?: string[];
}

export interface FraudAlertResponse {
  id: string;
  claimId: string;
  claimReference: string | null;
  assureNom: string | null;
  signalePar: string;
  signaleParNom: string | null;
  motif: string;
  description: string | null;
  niveauRisque: string | null;
  statut: string | null;
  piecesJustificatives: string[] | null;

  resoluPar: string | null;
  resoluParNom: string | null;
  decision: string | null;
  notesResolution: string | null;
  dateResolution: string | null;

  createdAt: string;
  updatedAt: string;
}

export interface FraudAlertResolutionRequest {
  decision: string;
  notesResolution?: string;
}

export interface FraudAlertStatsResponse {
  totalAlertes: number;
  alertesEnAttente: number;
  alertesEnCoursAnalyse: number;
  alertesConfirmees: number;
  alertesInfondees: number;
  alertesCritiques: number;
  alertesCeMois: number;
}

@Injectable({ providedIn: 'root' })
export class FraudAlertService {
  private readonly API_URL = '/api/fraud-alerts';
  // private readonly API_URL = '/api/fraud-alerts';

  constructor(private http: HttpClient) {}

  // Gestionnaire
  createFraudAlert(request: FraudAlertRequest): Observable<FraudAlertResponse> {
    return this.http.post<FraudAlertResponse>(this.API_URL, request);
  }

  getMyFraudAlerts(): Observable<FraudAlertResponse[]> {
    return this.http.get<FraudAlertResponse[]>(`${this.API_URL}/mes-signalements`);
  }

  getFraudAlertById(id: string): Observable<FraudAlertResponse> {
    return this.http.get<FraudAlertResponse>(`${this.API_URL}/${id}`);
  }

  // Admin
  getAllFraudAlerts(): Observable<FraudAlertResponse[]> {
    return this.http.get<FraudAlertResponse[]>(this.API_URL);
  }

  getPendingFraudAlerts(): Observable<FraudAlertResponse[]> {
    return this.http.get<FraudAlertResponse[]>(`${this.API_URL}/en-attente`);
  }

  startAnalysis(id: string): Observable<FraudAlertResponse> {
    return this.http.patch<FraudAlertResponse>(`${this.API_URL}/${id}/analyser`, {});
  }

  resolveFraudAlert(id: string, request: FraudAlertResolutionRequest): Observable<FraudAlertResponse> {
    return this.http.patch<FraudAlertResponse>(`${this.API_URL}/${id}/resoudre`, request);
  }

  getFraudAlertStats(): Observable<FraudAlertStatsResponse> {
    return this.http.get<FraudAlertStatsResponse>(`${this.API_URL}/stats`);
  }
}
