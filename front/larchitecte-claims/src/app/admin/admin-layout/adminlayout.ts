import { Component } from '@angular/core';
import { RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { AuthService } from '../../core/auth.service';
import { NotificationBell } from '../../shared/notification-bell/notificationbell';

@Component({
  selector: 'app-admin-layout',
  standalone: true,
  imports: [RouterLink, RouterLinkActive, RouterOutlet, NotificationBell],
  templateUrl: './adminlayout.html'
})
export class AdminLayout {
  constructor(public auth: AuthService) {}

  logout() {
    this.auth.logout();
  }
}
