import { Component, OnInit, signal, computed, effect } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { AuthService } from '../../core/auth.service';
import { MessageService, ConversationResponse, MessageResponse } from '../../core/message.service';

@Component({
  selector: 'app-messagerie',
  standalone: true,
  imports: [FormsModule],
  templateUrl: './messagerie.html'
})
export class MessagerieAssure implements OnInit {
  conversations = signal<ConversationResponse[]>([]);
  messages = signal<MessageResponse[]>([]);
  selectedConversationId = signal<string | null>(null);
  nouveauMessage = signal('');
  loading = signal(false);
  sending = signal(false);
  searchTerm = signal('');

  selectedConversation = computed(() => {
    const id = this.selectedConversationId();
    if (!id) return null;
    return this.conversations().find(c => c.id === id) ?? null;
  });

  filteredConversations = computed(() => {
    const term = this.searchTerm().toLowerCase();
    const convos = this.conversations();
    if (!term) return convos;
    return convos.filter(c =>
      c.participant1Nom.toLowerCase().includes(term) ||
      c.participant2Nom.toLowerCase().includes(term) ||
      (c.dernierMessage ?? '').toLowerCase().includes(term)
    );
  });

  otherParticipant = computed(() => {
    const conv = this.selectedConversation();
    if (!conv) return null;
    const myId = this.authService.userId();
    if (conv.participant1Id === myId) {
      return { nom: conv.participant1Nom, role: conv.participant1Role };
    }
    return { nom: conv.participant2Nom, role: conv.participant2Role };
  });

  constructor(
    private messageService: MessageService,
    public authService: AuthService
  ) {
    effect(() => {
      const msgs = this.messages();
      setTimeout(() => this.scrollToBottom(), 50);
    });
  }

  ngOnInit(): void {
    this.loadConversations();
  }

  loadConversations(): void {
    this.loading.set(true);
    this.messageService.getConversations().subscribe({
      next: (convos) => {
        this.conversations.set(convos);
        this.loading.set(false);
      },
      error: () => this.loading.set(false)
    });
  }

  selectConversation(conversationId: string): void {
    this.selectedConversationId.set(conversationId);
    this.messageService.getMessages(conversationId).subscribe({
      next: (msgs) => this.messages.set(msgs),
      error: () => this.messages.set([])
    });
    this.loadConversations();
  }

  envoyerMessage(): void {
    const contenu = this.nouveauMessage().trim();
    const conversationId = this.selectedConversationId();
    if (!contenu) return;

    this.sending.set(true);
    const request: any = { contenu };
    if (conversationId) {
      request.conversationId = conversationId;
    }

    this.messageService.envoyerMessage(request).subscribe({
      next: (msg) => {
        this.messages.update(msgs => [...msgs, msg]);
        this.nouveauMessage.set('');
        this.sending.set(false);
        this.loadConversations();
      },
      error: () => this.sending.set(false)
    });
  }

  formatTime(dateStr: string | null): string {
    if (!dateStr) return '';
    const date = new Date(dateStr);
    const now = new Date();
    const diffMs = now.getTime() - date.getTime();
    const diffDays = Math.floor(diffMs / (1000 * 60 * 60 * 24));

    if (diffDays === 0) {
      return date.toLocaleTimeString('fr-FR', { hour: '2-digit', minute: '2-digit' });
    } else if (diffDays === 1) {
      return 'Hier';
    } else if (diffDays < 7) {
      return date.toLocaleDateString('fr-FR', { weekday: 'short' });
    }
    return date.toLocaleDateString('fr-FR', { day: '2-digit', month: '2-digit' });
  }

  isMyMessage(msg: MessageResponse): boolean {
    return msg.expediteurId === this.authService.userId();
  }

  scrollToBottom(): void {
    const el = document.getElementById('chat-messages-assure');
    if (el) el.scrollTop = el.scrollHeight;
  }
}
