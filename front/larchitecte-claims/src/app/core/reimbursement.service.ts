import { Injectable, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';

export type MethodePaiement = 'CARTE_BANCAIRE';
export type StatutRemboursement = 'EN_ATTENTE' | 'VALIDEE' | 'EN_COURS_TRAITEMENT' | 'PAYE' | 'REFUSE';

export interface EtapeWorkflow {
  statut: StatutRemboursement;
  description: string;
  effectuePar: string;
  effectueParNom: string;
  date: string;
}

export interface ReimbursementRequest {
  claimId: string;
  montantDegats: number;
  capitalAssure: number;
  franchise: number;
  plafondGarantie: number;
  tauxRemboursement: number;
  typeSinistre?: string;
  montantPropose: number;
  methodePaiement?: MethodePaiement;
  justification?: string;
  notes?: string;
}

export interface ReimbursementResponse {
  id: string;
  claimId: string;
  claimReference: string;
  assureId: string;
  assureNom: string;
  reference: string;
  montantDegats: number;
  capitalAssure: number;
  franchise: number;
  plafondGarantie: number;
  tauxRemboursement: number;
  typeSinistre: string;
  montantApresFranchise: number;
  montantIndemnisationCalcule: number;
  detailCalcul: string;
  justification: string;
  montantPropose: number;
  montantFinal: number;
  methodePaiement: MethodePaiement;
  stripeSessionId: string;
  stripePaymentIntentId: string;
  transactionId: string;
  referencePaiement: string;
  confirmationPaiement: string;
  statut: StatutRemboursement;
  historiqueWorkflow: EtapeWorkflow[];
  gestionnaireId: string;
  gestionnaireNom: string;
  motifRefus: string;
  notes: string;
  dateProposition: string;
  dateValidation: string;
  dateTraitement: string;
  datePaiement: string;
  createdAt: string;
  updatedAt: string;
}

export interface CalculIndemnisationRequest {
  claimId: string;
  montantDegats: number;
  capitalAssure: number;
  franchise: number;
  plafondGarantie: number;
  tauxRemboursement: number;
  typeSinistre?: string;
}

export interface CalculIndemnisationResponse {
  claimId: string;
  montantDegats: number;
  franchise: number;
  montantApresFranchise: number;
  tauxRemboursement: number;
  montantIndemnisationCalcule: number;
  plafondGarantie: number;
  plafondAtteint: boolean;
  montantFinalCalcule: number;
  detailCalcul: string;
}

export interface PropositionIndemnisationResponse {
  id: string;
  claimId: string;
  claimReference: string;
  assureId: string;
  assureNom: string;
  reference: string;
  montantDegats: number;
  capitalAssure: number;
  franchise: number;
  plafondGarantie: number;
  tauxRemboursement: number;
  typeSinistre: string;
  montantApresFranchise: number;
  montantIndemnisationCalcule: number;
  detailCalcul: string;
  justification: string;
  montantPropose: number;
  montantFinal: number;
  methodePaiement: MethodePaiement;
  stripeSessionId: string;
  stripePaymentIntentId: string;
  transactionId: string;
  referencePaiement: string;
  confirmationPaiement: string;
  statut: StatutRemboursement;
  historiqueWorkflow: EtapeWorkflow[];
  gestionnaireId: string;
  gestionnaireNom: string;
  motifRefus: string;
  notes: string;
  dateProposition: string;
  dateValidation: string;
  dateTraitement: string;
  datePaiement: string;
  createdAt: string;
}

export interface RemboursementStats {
  totalRemboursements: number;
  enAttente: number;
  validees: number;
  enCoursTraitement: number;
  payes: number;
  refuses: number;
  montantTotalPaye: number;
  montantEnAttente: number;
}

@Injectable({ providedIn: 'root' })
export class ReimbursementService {
  private readonly API_URL = '/api/reimbursements';
  // private readonly API_URL = '/api/reimbursements';

  reimbursements = signal<ReimbursementResponse[]>([]);

  constructor(private http: HttpClient) {}

  // Calcul automatisé (prévisualisation)
  calculerIndemnisation(request: CalculIndemnisationRequest) {
    return this.http.post<CalculIndemnisationResponse>(`${this.API_URL}/calculer`, request);
  }

  createReimbursement(request: ReimbursementRequest) {
    return this.http.post<ReimbursementResponse>(this.API_URL, request);
  }

  // Proposition détaillée
  getPropositionDetaillee(id: string) {
    return this.http.get<PropositionIndemnisationResponse>(`${this.API_URL}/${id}/proposition`);
  }

  loadMyReimbursements() {
    this.http.get<ReimbursementResponse[]>(`${this.API_URL}/my`).subscribe({
      next: (data) => this.reimbursements.set(data),
      error: (err) => console.error('Erreur chargement remboursements', err)
    });
  }

  getAllReimbursements() {
    return this.http.get<ReimbursementResponse[]>(this.API_URL);
  }

  getReimbursementById(id: string) {
    return this.http.get<ReimbursementResponse>(`${this.API_URL}/${id}`);
  }

  getReimbursementsByStatut(statut: StatutRemboursement) {
    return this.http.get<ReimbursementResponse[]>(`${this.API_URL}/statut/${statut}`);
  }

  // Statistiques
  getStats() {
    return this.http.get<RemboursementStats>(`${this.API_URL}/stats`);
  }

  // Workflow
  validateReimbursement(id: string) {
    return this.http.put<ReimbursementResponse>(`${this.API_URL}/${id}/validate`, {});
  }

  refuseReimbursement(id: string, motif: string) {
    return this.http.put<ReimbursementResponse>(`${this.API_URL}/${id}/refuse`, motif, {
      headers: { 'Content-Type': 'application/json' }
    });
  }

  processReimbursement(id: string) {
    return this.http.put<ReimbursementResponse>(`${this.API_URL}/${id}/process`, {});
  }

  confirmPayment(id: string) {
    return this.http.put<ReimbursementResponse>(`${this.API_URL}/${id}/pay`, {});
  }

  // Stripe : création de session de paiement
  createStripeCheckoutSession(id: string) {
    return this.http.post<{sessionId: string; url: string}>(`${this.API_URL}/${id}/stripe/checkout`, {});
  }

  // Stripe : vérification du statut de paiement
  verifyStripeSession(id: string) {
    return this.http.get<{paymentStatus: string; [key: string]: any}>(`${this.API_URL}/${id}/stripe/verify`);
  }

  // Helpers
  getMethodeLabel(methode: MethodePaiement): string {
    return 'Carte bancaire (Stripe)';
  }

  getStatutLabel(statut: StatutRemboursement): string {
    switch (statut) {
      case 'EN_ATTENTE': return 'En attente';
      case 'VALIDEE': return 'Validée';
      case 'EN_COURS_TRAITEMENT': return 'En cours de traitement';
      case 'PAYE': return 'Payé';
      case 'REFUSE': return 'Refusé';
      default: return statut;
    }
  }

  getStatutColor(statut: StatutRemboursement): string {
    switch (statut) {
      case 'EN_ATTENTE': return 'bg-amber-100 text-amber-700';
      case 'VALIDEE': return 'bg-blue-100 text-blue-700';
      case 'EN_COURS_TRAITEMENT': return 'bg-purple-100 text-purple-700';
      case 'PAYE': return 'bg-emerald-100 text-emerald-700';
      case 'REFUSE': return 'bg-red-100 text-red-700';
      default: return 'bg-slate-100 text-slate-600';
    }
  }
}
