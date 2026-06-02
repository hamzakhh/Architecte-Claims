import { Component, inject, signal, OnInit, computed } from '@angular/core';
import { RouterLink } from '@angular/router';
import { CommonModule } from '@angular/common';
import { AdminService, AdminDashboardStatsResponse, AuditLogResponse, WorkloadResponse } from '../../core/admin.service';
import { ClaimService, ClaimResponse } from '../../core/claim.service';

@Component({
  selector: 'app-tableau-de-bord',
  standalone: true,
  imports: [RouterLink, CommonModule],
  templateUrl: './tableaudebord.html'
})
export class TableauDeBordAdmin implements OnInit {
  private adminService = inject(AdminService);
  private claimService = inject(ClaimService);

  stats = signal<AdminDashboardStatsResponse | null>(null);
  recentClaims = signal<ClaimResponse[]>([]);
  workload = signal<WorkloadResponse[]>([]);
  auditLogs = signal<AuditLogResponse[]>([]);
  loading = signal(true);
  error = signal('');

  activeSection = signal<'overview' | 'workload' | 'audit'>('overview');

  // SLA alerts: claims en cours older than 15 days
  slaAlerts = computed(() => {
    const fifteenDaysAgo = new Date();
    fifteenDaysAgo.setDate(fifteenDaysAgo.getDate() - 15);
    return this.recentClaims().filter(c =>
      c.statut === 'EN_COURS' && new Date(c.createdAt) < fifteenDaysAgo
    );
  });

  // KPIs
  tauxResolution = computed(() => {
    const s = this.stats();
    if (!s || s.totalSinistres === 0) return 0;
    return Math.round((s.sinistresClotures / s.totalSinistres) * 100);
  });

  coutMoyen = computed(() => {
    const claims = this.recentClaims();
    if (claims.length === 0) return '—';
    const total = claims.reduce((sum, c) => {
      const est = parseFloat((c.estimation || '0').replace(/[^0-9.,]/g, '').replace(',', '.'));
      return sum + (isNaN(est) ? 0 : est);
    }, 0);
    const avg = total / claims.length;
    return avg >= 1000 ? (avg / 1000).toFixed(1) + 'KDT' : Math.round(avg) + 'DT';
  });

  ngOnInit(): void {
    this.loadData();
  }

  loadData(): void {
    this.loading.set(true);
    this.adminService.getAdminStats().subscribe({
      next: (data) => {
        this.stats.set(data);
        this.loading.set(false);
      },
      error: () => {
        this.error.set('Erreur lors du chargement des statistiques');
        this.loading.set(false);
      }
    });
    this.claimService.getAllClaims().subscribe({
      next: (claims) => {
        const sorted = [...claims].sort((a, b) =>
          new Date(b.createdAt).getTime() - new Date(a.createdAt).getTime()
        );
        this.recentClaims.set(sorted);
      },
      error: () => {}
    });
    this.adminService.getWorkload().subscribe({
      next: (data) => this.workload.set(data),
      error: () => {}
    });
    this.adminService.getAuditLogs().subscribe({
      next: (data) => this.auditLogs.set(data.slice(0, 10)),
      error: () => {}
    });
  }

  setActiveSection(section: 'overview' | 'workload' | 'audit'): void {
    this.activeSection.set(section);
  }

  getChargeClass(pct: number): string {
    if (pct >= 90) return 'bg-error';
    if (pct >= 70) return 'bg-amber-500';
    return 'bg-emerald-500';
  }

  getChargeTextClass(pct: number): string {
    if (pct >= 90) return 'text-error';
    if (pct >= 70) return 'text-amber-600';
    return 'text-emerald-600';
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

  getStatutLabel(statut: string): string {
    const labels: Record<string, string> = {
      'EN_COURS': 'En cours',
      'EN_REVISION': 'En révision',
      'EXPERTISE': 'Expertise',
      'VALIDE': 'Validé',
      'REFUSE': 'Refusé',
      'CLOTURE': 'Clôturé'
    };
    return labels[statut] || statut;
  }

  getStatutClass(statut: string): string {
    const classes: Record<string, string> = {
      'EN_COURS': 'bg-tertiary-fixed text-on-tertiary-fixed-variant',
      'EN_REVISION': 'bg-amber-100 text-amber-700',
      'EXPERTISE': 'bg-blue-100 text-blue-700',
      'VALIDE': 'bg-emerald-100 text-emerald-700',
      'REFUSE': 'bg-error-container text-on-error-container',
      'CLOTURE': 'bg-slate-100 text-slate-600'
    };
    return classes[statut] || 'bg-slate-100 text-slate-600';
  }

  getTypeLabel(type: string): string {
    const labels: Record<string, string> = {
      'water': 'Dégât des Eaux',
      'fire': 'Incendie',
      'theft': 'Vol',
      'auto': 'Auto',
      'natural': 'Catastrophe'
    };
    return labels[type] || type;
  }

  formatDate(dateStr: string): string {
    if (!dateStr) return '—';
    try {
      const date = new Date(dateStr);
      return date.toLocaleDateString('fr-FR', { day: 'numeric', month: 'short', year: 'numeric', hour: '2-digit', minute: '2-digit' });
    } catch {
      return dateStr;
    }
  }

  formatShortDate(dateStr: string): string {
    if (!dateStr) return '—';
    try {
      const date = new Date(dateStr);
      return date.toLocaleDateString('fr-FR', { day: '2-digit', month: 'short', hour: '2-digit', minute: '2-digit' });
    } catch {
      return dateStr;
    }
  }

  formatNumber(n: number | undefined): string {
    if (n === undefined || n === null) return '0';
    return n.toLocaleString('fr-FR');
  }
}
