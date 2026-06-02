import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface AnalyseIARequest {
  claimId: string;
  typeAnalyse?: string;
}

export interface AnalyseIAResponse {
  id: string;
  claimId: string;
  claimReference: string;

  scoreComplexite: number;
  scoreRisque: number;
  scoreConfiance: number;

  montantEstime: number | null;
  devise: string | null;

  severite: string | null;
  categorieDetectee: string | null;
  motsCles: string[] | null;

  necessiteExpertHumain: boolean;
  recommandation: string | null;
  justification: string | null;

  resumeAnalyse: string | null;
  pointsAttention: string | null;
  elementsFraude: string | null;
  recommandationsAction: string | null;

  typeAnalyse: string | null;
  statut: string | null;
  modeleIA: string | null;
  tokensUtilises: number;
  coutEstime: number;

  dateAnalyse: string | null;
  createdAt: string | null;
}

export interface AnalyseIAStats {
  totalAnalyses: number;
  analysesTerminees: number;
  analysesEnCours: number;
  analysesErreur: number;
  necessitantExpert: number;
  critiques: number;
  tauxExpertRequis: number;
  ollamaEnabled: boolean;
  modeleIA: string;
}

@Injectable({ providedIn: 'root' })
export class AnalyseIAService {
  private readonly API_URL = '/api/analyses-ia';
  // private readonly API_URL = '/api/analyses-ia'; 

  constructor(private http: HttpClient) {}

  analyserSinistre(request: AnalyseIARequest): Observable<AnalyseIAResponse> {
    return this.http.post<AnalyseIAResponse>(this.API_URL, request);
  }

  getAnalyseByClaimId(claimId: string): Observable<AnalyseIAResponse> {
    return this.http.get<AnalyseIAResponse>(`${this.API_URL}/claim/${claimId}`);
  }

  getAnalysesNecessitantExpert(): Observable<AnalyseIAResponse[]> {
    return this.http.get<AnalyseIAResponse[]>(`${this.API_URL}/expert-requis`);
  }

  getAllAnalyses(): Observable<AnalyseIAResponse[]> {
    return this.http.get<AnalyseIAResponse[]>(this.API_URL);
  }

  getStats(): Observable<AnalyseIAStats> {
    return this.http.get<AnalyseIAStats>(`${this.API_URL}/stats`);
  }
}
