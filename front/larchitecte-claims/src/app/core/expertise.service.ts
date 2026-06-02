import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface ExpertiseRequest {
  claimId: string;
  conclusion?: string;
  montantEstime?: string;
  recommandation?: string;
  piecesJointes?: string[];
}

export interface ExpertiseResponse {
  id: string;
  claimId: string;
  expertId: string;
  gestionnaireId: string;
  conclusion: string;
  montantEstime: string;
  recommandation: string;
  piecesJointes: string[] | null;
  statut: string;
  dateRapport: string;
  createdAt: string;
  updatedAt: string;
  expertNom: string;
  gestionnaireNom: string;
  claimReference: string;
}

@Injectable({ providedIn: 'root' })
export class ExpertiseService {
  private readonly API_URL = '/api/expertises';
  // private readonly API_URL = '/api/expertises';

  constructor(private http: HttpClient) {}

  createExpertise(request: ExpertiseRequest): Observable<ExpertiseResponse> {
    return this.http.post<ExpertiseResponse>(this.API_URL, request);
  }

  getExpertiseById(id: string): Observable<ExpertiseResponse> {
    return this.http.get<ExpertiseResponse>(`${this.API_URL}/${id}`);
  }

  getExpertisesByClaim(claimId: string): Observable<ExpertiseResponse[]> {
    return this.http.get<ExpertiseResponse[]>(`${this.API_URL}/claim/${claimId}`);
  }

  getExpertisesByGestionnaire(): Observable<ExpertiseResponse[]> {
    return this.http.get<ExpertiseResponse[]>(`${this.API_URL}/gestionnaire`);
  }

  submitRapport(expertiseId: string, request: ExpertiseRequest): Observable<ExpertiseResponse> {
    return this.http.put<ExpertiseResponse>(`${this.API_URL}/${expertiseId}/rapport`, request);
  }

  saveBrouillon(expertiseId: string, request: ExpertiseRequest): Observable<ExpertiseResponse> {
    return this.http.put<ExpertiseResponse>(`${this.API_URL}/${expertiseId}/brouillon`, request);
  }

  validerExpertise(expertiseId: string): Observable<ExpertiseResponse> {
    return this.http.patch<ExpertiseResponse>(`${this.API_URL}/${expertiseId}/valider`, {});
  }

  refuserExpertise(expertiseId: string): Observable<ExpertiseResponse> {
    return this.http.patch<ExpertiseResponse>(`${this.API_URL}/${expertiseId}/refuser`, {});
  }

  getMyExpertises(): Observable<ExpertiseResponse[]> {
    return this.http.get<ExpertiseResponse[]>(`${this.API_URL}/mes-expertises`);
  }
}
