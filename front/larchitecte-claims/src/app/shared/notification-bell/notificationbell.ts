import { Component, OnInit, inject, signal, DestroyRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { NotificationService, NotificationResponse } from '../../core/notification.service';

@Component({
  selector: 'app-notification-bell',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './notificationbell.html'
})
export class NotificationBell implements OnInit {
  private destroyRef = inject(DestroyRef);
  private notifService = inject(NotificationService);

  showDropdown = signal(false);
  notifications = signal<NotificationResponse[]>([]);

  ngOnInit(): void {
    this.notifService.loadNotifications();
  }

  get unreadCount(): number {
    return this.notifService.unreadCount();
  }

  get allNotifications(): NotificationResponse[] {
    return this.notifService.notifications();
  }

  toggleDropdown(): void {
    this.showDropdown.update(v => !v);
    if (this.showDropdown()) {
      this.notifService.loadNotifications();
    }
  }

  closeDropdown(): void {
    this.showDropdown.set(false);
  }

  markAsRead(id: string): void {
    this.notifService.markAsRead(id)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: () => this.notifService.loadNotifications(),
        error: (err) => console.error('Erreur marquage lu', err)
      });
  }

  markAllAsRead(): void {
    this.notifService.markAllAsRead()
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: () => this.notifService.loadNotifications(),
        error: (err) => console.error('Erreur marquage tout lu', err)
      });
  }

  formatDate(dateString: string): string {
    if (!dateString) return '';
    const date = new Date(dateString);
    const now = new Date();
    const diff = now.getTime() - date.getTime();
    const minutes = Math.floor(diff / 60000);
    if (minutes < 1) return "À l'instant";
    if (minutes < 60) return `Il y a ${minutes} min`;
    const hours = Math.floor(minutes / 60);
    if (hours < 24) return `Il y a ${hours}h`;
    return date.toLocaleDateString('fr-FR', { day: 'numeric', month: 'short' });
  }

  getTypeIcon(type: string): string {
    switch (type) {
      case 'STATUT_CHANGE': return 'swap_horiz';
      case 'ASSIGNATION': return 'person_add';
      case 'REMBOURSEMENT': return 'payments';
      case 'EXPERTISE': return 'engineering';
      case 'RAPPEL': return 'notifications_active';
      default: return 'info';
    }
  }
}
