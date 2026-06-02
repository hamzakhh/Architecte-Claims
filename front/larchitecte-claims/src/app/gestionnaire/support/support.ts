import { Component, OnInit, inject, signal, DestroyRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { TicketService, TicketResponse, TicketMessageRequest } from '../../core/ticket.service';

@Component({
  selector: 'app-support-gestionnaire',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './support.html'
})
export class SupportGestionnaire implements OnInit {
  private destroyRef = inject(DestroyRef);
  private ticketService = inject(TicketService);

  tickets = signal<TicketResponse[]>([]);
  loading = signal(true);
  selectedTicket = signal<TicketResponse | null>(null);
  newMessage = '';
  filterStatut = '';

  ngOnInit(): void {
    this.ticketService.getAllTickets()
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (data) => { this.tickets.set(data); this.loading.set(false); },
        error: () => this.loading.set(false)
      });
  }

  get filteredTickets(): TicketResponse[] {
    const t = this.tickets();
    if (!this.filterStatut) return t;
    return t.filter(tk => tk.statut === this.filterStatut);
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

  assignTicket(ticket: TicketResponse): void {
    this.ticketService.assignTicket(ticket.id, 'self')
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: () => this.refreshList(),
        error: (err) => console.error('Erreur assignation', err)
      });
  }

  resolveTicket(ticket: TicketResponse): void {
    this.ticketService.resolveTicket(ticket.id)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: () => this.refreshList(),
        error: (err) => console.error('Erreur résolution', err)
      });
  }

  closeTicketStatus(ticket: TicketResponse): void {
    this.ticketService.closeTicket(ticket.id)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: () => this.refreshList(),
        error: (err) => console.error('Erreur fermeture', err)
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
          this.refreshList();
        },
        error: (err) => console.error('Erreur envoi message', err)
      });
  }

  private refreshList(): void {
    this.ticketService.getAllTickets()
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({ next: (data) => this.tickets.set(data) });
  }
}
