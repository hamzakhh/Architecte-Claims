import { Component, inject, signal, OnInit, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { AdminService, AuditLogResponse } from '../../core/admin.service';

@Component({
  selector: 'app-gestion-technique',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './gestiontechnique.html'
})
export class GestionTechnique implements OnInit {
  private adminService = inject(AdminService);

  logs = signal<AuditLogResponse[]>([]);
  loading = signal(true);
  error = signal('');

  filterAction = signal('');
  filterRole = signal('');
  searchTerm = signal('');

  filteredLogs = computed(() => {
    let result = this.logs();
    const action = this.filterAction();
    const role = this.filterRole();
    const term = this.searchTerm().toLowerCase();

    if (action) {
      result = result.filter(l => l.action === action);
    }
    if (role) {
      result = result.filter(l => l.role === role);
    }
    if (term) {
      result = result.filter(l =>
        l.utilisateur.toLowerCase().includes(term) ||
        l.cible.toLowerCase().includes(term) ||
        l.details.toLowerCase().includes(term)
      );
    }
    return result;
  });

  uniqueActions = computed(() => [...new Set(this.logs().map(l => l.action))]);
  uniqueRoles = computed(() => [...new Set(this.logs().map(l => l.role))]);

  ngOnInit(): void {
    this.loadData();
  }

  loadData(): void {
    this.loading.set(true);
    this.error.set('');
    this.adminService.getAuditLogs().subscribe({
      next: (data) => {
        this.logs.set(data);
        this.loading.set(false);
      },
      error: () => {
        this.error.set('Erreur lors du chargement des logs système');
        this.loading.set(false);
      }
    });
  }

  getActionLabel(action: string): string {
    const labels: Record<string, string> = {
      'CREATION_SINISTRE': 'Déclaration sinistre',
      'MISE_A_JOUR_STATUT': 'Mise à jour statut',
      'CREATION_UTILISATEUR': 'Création utilisateur'
    };
    return labels[action] || action;
  }

  getActionClass(action: string): string {
    if (action === 'CREATION_SINISTRE') return 'bg-primary/10 text-primary';
    if (action === 'MISE_A_JOUR_STATUT') return 'bg-amber-100 text-amber-700';
    if (action === 'CREATION_UTILISATEUR') return 'bg-emerald-100 text-emerald-700';
    return 'bg-slate-100 text-slate-600';
  }

  getRoleClass(role: string): string {
    switch (role) {
      case 'ADMIN': return 'bg-blue-100 text-blue-700';
      case 'GESTIONNAIRE': return 'bg-emerald-100 text-emerald-700';
      case 'EXPERT': return 'bg-amber-100 text-amber-700';
      case 'ASSURE': return 'bg-primary/10 text-primary';
      case 'SYSTEME': return 'bg-slate-100 text-slate-600';
      default: return 'bg-slate-100 text-slate-600';
    }
  }

  getRoleLabel(role: string): string {
    const labels: Record<string, string> = {
      'ADMIN': 'Admin',
      'GESTIONNAIRE': 'Gestionnaire',
      'EXPERT': 'Expert',
      'ASSURE': 'Assuré',
      'SYSTEME': 'Système'
    };
    return labels[role] || role;
  }

  formatDate(dateStr: string): string {
    if (!dateStr) return '—';
    try {
      const date = new Date(dateStr);
      return date.toLocaleDateString('fr-FR', { day: '2-digit', month: 'short', year: 'numeric', hour: '2-digit', minute: '2-digit', second: '2-digit' });
    } catch {
      return dateStr;
    }
  }

  resetFilters(): void {
    this.filterAction.set('');
    this.filterRole.set('');
    this.searchTerm.set('');
  }
}
