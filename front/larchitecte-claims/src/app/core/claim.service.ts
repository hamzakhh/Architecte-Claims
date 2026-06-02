import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface ClaimRequest {
  type: string;
  description: string;
  dateSinistre: string;
  heureSinistre?: string;
  lieu?: string;
  notesLieu?: string;
  estimation?: string;
  latitude?: number;
  longitude?: number;
  piecesJointes?: string[];
}

export interface ClaimResponse {
  id: string;
  reference: string;
  assureId: string;
  assureNom: string;
  categorie: string;
  type: string;
  latitude: number;
  longitude: number;
  description: string;
  dateSinistre: string;
  heureSinistre: string;
  lieu: string;
  notesLieu: string;
  piecesJointes: string[];
  estimation: string;
  notesInternes: string;

  // Qualification (2.2.1)
  gravite: string | null;
  couvertureContractuelle: string | null;
  franchise: number | null;
  plafondCouverture: number | null;

  // Indemnisation (2.2.4)
  montantIndemnisationPropose: number | null;
  montantIndemnisationFinal: number | null;
  motifIndemnisation: string | null;
  indemnisationAcceptee: boolean | null;
  motifRefusIndemnisation: string | null;
  recoursEnCours: boolean | null;
  datePaiement: string | null;

  statut: string;
  gestionnaireId: string | null;
  gestionnaireNom: string | null;
  expertId: string | null;
  expertNom: string | null;
  analyseIAId: string | null;
  createdAt: string;
  updatedAt: string;
}

export interface ExpertProfileResponse {
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

export interface DashboardStatsResponse {
  dossiersEnCours: number;
  enAttenteApprobation: number;
  expertisesEnCours: number;
  expertsDisponibles: number;
  totalExperts: number;
  sinistresCeMois: number;
  dossiersOuverts: number;
  dossiersEnAttente: number;
  dossiersUrgence: number;
  dossiersSansGestionnaire: number;
}

export interface ExpertDashboardStatsResponse {
  missionsEnCours: number;
  rapportsARendre: number;
  completesCeMois: number;
  totalMissions: number;
}

export interface ClaimHistoryEntry {
  id: string;
  claimId: string;
  action: string;
  description: string;
  utilisateurId: string | null;
  utilisateurNom: string;
  utilisateurRole: string;
  ancienStatut: string | null;
  nouveauStatut: string | null;
  createdAt: string;
}

@Injectable({ providedIn: 'root' })
export class ClaimService {
  private readonly API_URL = '/api/claims';
  // private readonly API_URL = '/api/claims';

  constructor(private http: HttpClient) {}

  createClaim(request: ClaimRequest): Observable<ClaimResponse> {
    return this.http.post<ClaimResponse>(this.API_URL, request);
  }

  getMyClaims(): Observable<ClaimResponse[]> {
    return this.http.get<ClaimResponse[]>(`${this.API_URL}/mes-sinistres`);
  }

  getClaimById(id: string): Observable<ClaimResponse> {
    return this.http.get<ClaimResponse>(`${this.API_URL}/${id}`);
  }

  getAllClaims(): Observable<ClaimResponse[]> {
    return this.http.get<ClaimResponse[]>(this.API_URL);
  }

  getAssignedClaims(): Observable<ClaimResponse[]> {
    return this.http.get<ClaimResponse[]>(`${this.API_URL}/dossiers-assignes`);
  }

  getDashboardStats(): Observable<DashboardStatsResponse> {
    return this.http.get<DashboardStatsResponse>(`${this.API_URL}/dashboard-stats`);
  }

  getExperts(): Observable<ExpertProfileResponse[]> {
    return this.http.get<ExpertProfileResponse[]>(`${this.API_URL}/experts`);
  }

  toggleExpertStatus(expertId: string): Observable<ExpertProfileResponse> {
    return this.http.patch<ExpertProfileResponse>(`/api/users/${expertId}/toggle-status`, {});
  }

  prendreEnCharge(claimId: string): Observable<ClaimResponse> {
    return this.http.patch<ClaimResponse>(`${this.API_URL}/${claimId}/prendre-en-charge`, {});
  }

  assignExpert(claimId: string, expertId: string): Observable<ClaimResponse> {
    return this.http.patch<ClaimResponse>(`${this.API_URL}/${claimId}/assign-expert`, expertId);
  }

  autoAssignExpert(claimId: string): Observable<ClaimResponse> {
    return this.http.patch<ClaimResponse>(`${this.API_URL}/${claimId}/auto-assign-expert`, {});
  }

  updateClaimStatus(claimId: string, statut: string): Observable<ClaimResponse> {
    return this.http.patch<ClaimResponse>(`${this.API_URL}/${claimId}/statut`, { statut });
  }

  getExpertDossiers(): Observable<ClaimResponse[]> {
    return this.http.get<ClaimResponse[]>(`${this.API_URL}/mes-dossiers`);
  }

  getExpertDashboardStats(): Observable<ExpertDashboardStatsResponse> {
    return this.http.get<ExpertDashboardStatsResponse>(`${this.API_URL}/expert-dashboard-stats`);
  }

  getGestionnaires(): Observable<ExpertProfileResponse[]> {
    return this.http.get<ExpertProfileResponse[]>(`${this.API_URL}/gestionnaires`);
  }

  transfererGestionnaire(claimId: string, nouveauGestionnaireId: string): Observable<ClaimResponse> {
    return this.http.patch<ClaimResponse>(`${this.API_URL}/${claimId}/transferer-gestionnaire`, nouveauGestionnaireId);
  }

  updateNotesInternes(claimId: string, notes: string): Observable<ClaimResponse> {
    return this.http.patch<ClaimResponse>(`${this.API_URL}/${claimId}/notes-internes`, notes);
  }

  archiverSinistre(claimId: string): Observable<ClaimResponse> {
    return this.http.patch<ClaimResponse>(`${this.API_URL}/${claimId}/archiver`, {});
  }

  // Qualification (2.2.1)
  qualifierSinistre(claimId: string, request: QualificationRequest): Observable<ClaimResponse> {
    return this.http.patch<ClaimResponse>(`${this.API_URL}/${claimId}/qualifier`, request);
  }

  // Indemnisation (2.2.4)
  proposerIndemnisation(claimId: string, request: IndemnisationRequest): Observable<ClaimResponse> {
    return this.http.patch<ClaimResponse>(`${this.API_URL}/${claimId}/proposer-indemnisation`, request);
  }

  accepterIndemnisation(claimId: string): Observable<ClaimResponse> {
    return this.http.patch<ClaimResponse>(`${this.API_URL}/${claimId}/accepter-indemnisation`, {});
  }

  refuserIndemnisation(claimId: string, motifRefus: string): Observable<ClaimResponse> {
    return this.http.patch<ClaimResponse>(`${this.API_URL}/${claimId}/refuser-indemnisation`, motifRefus);
  }

  initierPaiement(claimId: string): Observable<ClaimResponse> {
    return this.http.patch<ClaimResponse>(`${this.API_URL}/${claimId}/initier-paiement`, {});
  }

  confirmerPaiement(claimId: string): Observable<ClaimResponse> {
    return this.http.patch<ClaimResponse>(`${this.API_URL}/${claimId}/confirmer-paiement`, {});
  }

  declarerRecours(claimId: string): Observable<ClaimResponse> {
    return this.http.patch<ClaimResponse>(`${this.API_URL}/${claimId}/declarer-recours`, {});
  }

  getClaimHistory(claimId: string): Observable<ClaimHistoryEntry[]> {
    return this.http.get<ClaimHistoryEntry[]>(`${this.API_URL}/${claimId}/history`);
  }
}

export interface QualificationRequest {
  gravite: string;
  couvertureContractuelle?: string;
  franchise?: number;
  plafondCouverture?: number;
}

export interface IndemnisationRequest {
  montantPropose: number;
  motifIndemnisation?: string;
}
