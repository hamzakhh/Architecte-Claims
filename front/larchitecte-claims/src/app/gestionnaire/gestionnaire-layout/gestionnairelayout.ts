import { Component } from '@angular/core';
import { RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { AuthService } from '../../core/auth.service';
import { NotificationBell } from '../../shared/notification-bell/notificationbell';

@Component({
  selector: 'app-gestionnaire-layout',
  standalone: true,
  imports: [RouterOutlet, RouterLink, RouterLinkActive, NotificationBell],
  templateUrl: './gestionnairelayout.html'
})
export class GestionnaireLayout {
  constructor(public auth: AuthService) {}

  logout() {
    this.auth.logout();
  }
}
