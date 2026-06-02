import { Component, OnInit, inject, signal, DestroyRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { TicketService, TicketResponse, TicketRequest, TicketMessageRequest } from '../../core/ticket.service';

@Component({
  selector: 'app-support-assure',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './support.html'
})
export class SupportAssure implements OnInit {
  private destroyRef = inject(DestroyRef);
  private ticketService = inject(TicketService);

  tickets = signal<TicketResponse[]>([]);
  loading = signal(true);
  selectedTicket = signal<TicketResponse | null>(null);
  showNewTicket = signal(false);
  newMessage = '';

  newTicket: TicketRequest = { sujet: '', description: '', categorie: 'AUTRE' };

  ngOnInit(): void {
    this.ticketService.loadMyTickets();
    setTimeout(() => this.loading.set(false), 1000);
  }

  get ticketsList(): TicketResponse[] {
    return this.ticketService.tickets();
  }

  getStatutLabel(statut: string): string {
    switch (statut) {
      case 'OUVERT': return 'Ouvert';
      case 'EN_COURS': return 'En cours';
      case 'RESOLU': return 'Résolu';
      case 'FERME': return 'Fermé';
      default: return statut;
    }
  }

  getStatutColor(statut: string): string {
    switch (statut) {
      case 'OUVERT': return 'bg-amber-100 text-amber-700';
      case 'EN_COURS': return 'bg-blue-100 text-blue-700';
      case 'RESOLU': return 'bg-emerald-100 text-emerald-700';
      case 'FERME': return 'bg-slate-100 text-slate-600';
      default: return 'bg-slate-100 text-slate-600';
    }
  }

  formatDate(dateString: string): string {
    if (!dateString) return 'N/A';
    return new Date(dateString).toLocaleDateString('fr-FR', { day: 'numeric', month: 'long', year: 'numeric', hour: '2-digit', minute: '2-digit' });
  }

  openTicket(ticket: TicketResponse): void {
    this.selectedTicket.set(ticket);
    this.newMessage = '';
  }

  closeTicket(): void {
    this.selectedTicket.set(null);
  }

  toggleNewTicket(): void {
    this.showNewTicket.update(v => !v);
    if (this.showNewTicket()) {
      this.newTicket = { sujet: '', description: '', categorie: 'AUTRE' };
    }
  }

  submitTicket(): void {
    if (!this.newTicket.sujet || !this.newTicket.description) return;
    this.ticketService.createTicket(this.newTicket)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: () => {
          this.showNewTicket.set(false);
          this.ticketService.loadMyTickets();
        },
        error: (err) => console.error('Erreur création ticket', err)
      });
  }

  sendMessage(): void {
    const ticket = this.selectedTicket();
    if (!ticket || !this.newMessage.trim()) return;
    const request: TicketMessageRequest = { contenu: this.newMessage.trim() };
    this.ticketService.addMessage(ticket.id, request)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (updated) => {
          this.selectedTicket.set(updated);
          this.newMessage = '';
        },
        error: (err) => console.error('Erreur envoi message', err)
      });
  }
}
