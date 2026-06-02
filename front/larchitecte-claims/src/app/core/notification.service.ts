import { Injectable, signal, computed } from '@angular/core';
import { HttpClient } from '@angular/common/http';

export interface NotificationResponse {
  id: string;
  utilisateurId: string;
  titre: string;
  message: string;
  type: string;
  claimId: string;
  lu: boolean;
  createdAt: string;
}

@Injectable({ providedIn: 'root' })
export class NotificationService {
  private readonly API_URL = '/api/notifications';
  // private readonly API_URL = '/api/notifications';

  notifications = signal<NotificationResponse[]>([]);
  unreadCount = computed(() => this.notifications().filter(n => !n.lu).length);

  constructor(private http: HttpClient) {}

  loadNotifications() {
    this.http.get<NotificationResponse[]>(this.API_URL).subscribe({
      next: (data) => this.notifications.set(data),
      error: (err) => console.error('Erreur chargement notifications', err)
    });
  }

  getUnread() {
    return this.http.get<NotificationResponse[]>(`${this.API_URL}/unread`);
  }

  getUnreadCount() {
    return this.http.get<number>(`${this.API_URL}/unread-count`);
  }

  markAsRead(id: string) {
    return this.http.put<NotificationResponse>(`${this.API_URL}/${id}/read`, {});
  }

  markAllAsRead() {
    return this.http.put(`${this.API_URL}/read-all`, {});
  }

  refreshUnreadCount() {
    this.http.get<number>(`${this.API_URL}/unread-count`).subscribe({
      next: (count) => {
        // Re-assign to trigger computed signal recalculation
        const all = [...this.notifications()];
        this.notifications.set(all);
      }
    });
  }
}
