import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface ConversationResponse {
  id: string;
  participant1Id: string;
  participant1Nom: string;
  participant1Role: string;
  participant2Id: string;
  participant2Nom: string;
  participant2Role: string;
  claimId: string | null;
  dernierMessage: string | null;
  dernierMessageDate: string | null;
  messagesNonLus: number;
  createdAt: string;
}

export interface MessageRequest {
  contenu: string;
  conversationId?: string | null;
  destinataireId?: string | null;
}

export interface MessageResponse {
  id: string;
  conversationId: string;
  expediteurId: string;
  expediteurNom: string;
  expediteurRole: string;
  contenu: string;
  lu: boolean;
  createdAt: string;
}

@Injectable({ providedIn: 'root' })
export class MessageService {
  private readonly API_URL = '/api/messages';
  // private readonly API_URL = '/api/messages';

  constructor(private http: HttpClient) {}

  envoyerMessage(request: MessageRequest): Observable<MessageResponse> {
    return this.http.post<MessageResponse>(this.API_URL, request);
  }

  getConversations(): Observable<ConversationResponse[]> {
    return this.http.get<ConversationResponse[]>(`${this.API_URL}/conversations`);
  }

  getMessages(conversationId: string): Observable<MessageResponse[]> {
    return this.http.get<MessageResponse[]>(`${this.API_URL}/conversations/${conversationId}`);
  }
}
