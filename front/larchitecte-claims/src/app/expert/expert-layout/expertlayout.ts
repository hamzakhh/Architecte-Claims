import { Component } from '@angular/core';
import { RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { AuthService } from '../../core/auth.service';
import { NotificationBell } from '../../shared/notification-bell/notificationbell';

@Component({
  selector: 'app-expert-layout',
  standalone: true,
  imports: [RouterOutlet, RouterLink, RouterLinkActive, NotificationBell],
  templateUrl: './expertlayout.html'
})
export class ExpertLayout {
  constructor(public auth: AuthService) {}

  logout() {
    this.auth.logout();
  }
}
