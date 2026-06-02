import { Component } from '@angular/core';
import { RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { AuthService } from '../../core/auth.service';
import { NotificationBell } from '../../shared/notification-bell/notificationbell';

@Component({
  selector: 'app-assure-layout',
  standalone: true,
  imports: [RouterOutlet, RouterLink, RouterLinkActive, NotificationBell],
  templateUrl: './assurelayout.html'
})
export class AssureLayout {
  constructor(public auth: AuthService) {}

  logout() {
    this.auth.logout();
  }
}
