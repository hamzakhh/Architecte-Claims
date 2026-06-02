import { Injectable, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';

export interface TicketRequest {
  sujet: string;
  description: string;
  categorie?: string;
  claimId?: string;
}

export interface TicketMessageRequest {
  contenu: string;
}

export interface TicketMessageResponse {
  expediteurId: string;
  expediteurNom: string;
  contenu: string;
  createdAt: string;
}

export interface TicketResponse {
  id: string;
  assureId: string;
  assureNom: string;
  claimId: string;
  claimReference: string;
  sujet: string;
  description: string;
  categorie: string;
  statut: string;
  assigneA: string;
  assigneNom: string;
  messages: TicketMessageResponse[];
  createdAt: string;
  updatedAt: string;
}

@Injectable({ providedIn: 'root' })
export class TicketService {
  private readonly API_URL = '/api/tickets';
  // private readonly API_URL = '/api/tickets';

  tickets = signal<TicketResponse[]>([]);

  constructor(private http: HttpClient) {}

  createTicket(request: TicketRequest) {
    return this.http.post<TicketResponse>(this.API_URL, request);
  }

  loadMyTickets() {
    this.http.get<TicketResponse[]>(`${this.API_URL}/my`).subscribe({
      next: (data) => this.tickets.set(data),
      error: (err) => console.error('Erreur chargement tickets', err)
    });
  }

  getAllTickets() {
    return this.http.get<TicketResponse[]>(this.API_URL);
  }

  getTicketById(id: string) {
    return this.http.get<TicketResponse>(`${this.API_URL}/${id}`);
  }

  addMessage(ticketId: string, request: TicketMessageRequest) {
    return this.http.post<TicketResponse>(`${this.API_URL}/${ticketId}/messages`, request);
  }

  assignTicket(ticketId: string, gestionnaireId: string) {
    return this.http.put<TicketResponse>(`${this.API_URL}/${ticketId}/assign?gestionnaireId=${gestionnaireId}`, {});
  }

  resolveTicket(ticketId: string) {
    return this.http.put<TicketResponse>(`${this.API_URL}/${ticketId}/resolve`, {});
  }

  closeTicket(ticketId: string) {
    return this.http.put<TicketResponse>(`${this.API_URL}/${ticketId}/close`, {});
  }
}
