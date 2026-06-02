import { Component, inject, signal, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { AdminService, AnalyticsStatsResponse, TypeStat, MonthlyStat, RegionStat, FinancialIndicator } from '../../core/admin.service';

@Component({
  selector: 'app-rapports-analytics',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './rapportsanalytics.html'
})
export class RapportsAnalytics implements OnInit {
  private adminService = inject(AdminService);

  stats = signal<AnalyticsStatsResponse | null>(null);
  loading = signal(true);
  error = signal('');
  selectedPeriod = signal('year');

  ngOnInit(): void {
    this.loadData();
  }

  loadData(): void {
    this.loading.set(true);
    this.adminService.getAnalyticsStats().subscribe({
      next: (data) => {
        this.stats.set(data);
        this.loading.set(false);
      },
      error: () => {
        this.error.set('Erreur lors du chargement des statistiques');
        this.loading.set(false);
      }
    });
  }

  formatNumber(n: number | undefined): string {
    if (n === undefined || n === null) return '0';
    return n.toLocaleString('fr-FR');
  }

  formatCurrency(n: number | undefined): string {
    if (n === undefined || n === null) return '0 DT';
    if (n >= 1000000) return (n / 1000000).toFixed(1) + 'MDT';
    if (n >= 1000) return (n / 1000).toFixed(0) + 'KDT';
    return n.toLocaleString('fr-FR') + ' DT';
  }

  getTypeBarColor(type: string): string {
    const colors: Record<string, string> = {
      'water': 'bg-primary',
      'fire': 'bg-error',
      'theft': 'bg-on-tertiary-container',
      'auto': 'bg-amber-500',
      'natural': 'bg-emerald-500',
      'autre': 'bg-slate-400'
    };
    return colors[type] || 'bg-slate-400';
  }

  getTypeTextColor(type: string): string {
    const colors: Record<string, string> = {
      'water': 'text-primary',
      'fire': 'text-error',
      'theft': 'text-on-tertiary-container',
      'auto': 'text-amber-600',
      'natural': 'text-emerald-600',
      'autre': 'text-slate-400'
    };
    return colors[type] || 'text-slate-400';
  }

  getMaxMonthlyCount(): number {
    const data = this.stats()?.tendanceMensuelle;
    if (!data || data.length === 0) return 100;
    return Math.max(...data.map(m => m.count), 1);
  }

  getBarHeight(count: number): string {
    const max = this.getMaxMonthlyCount();
    return max > 0 ? Math.max(5, (count / max) * 100) + '%' : '5%';
  }

  getBarOpacity(count: number): string {
    const max = this.getMaxMonthlyCount();
    const ratio = max > 0 ? count / max : 0;
    if (ratio > 0.8) return 'bg-primary';
    if (ratio > 0.6) return 'bg-primary/70';
    if (ratio > 0.4) return 'bg-primary/50';
    return 'bg-primary/30';
  }

  getSatisfactionColor(satisfaction: number): string {
    if (satisfaction >= 90) return 'text-emerald-600';
    if (satisfaction >= 80) return 'text-on-tertiary-container';
    return 'text-amber-600';
  }

  getTrendIcon(trend: string): string {
    if (trend === 'up') return 'trending_up';
    if (trend === 'down') return 'trending_down';
    return 'trending_flat';
  }

  getTrendColor(trend: string): string {
    if (trend === 'up') return 'text-error';
    if (trend === 'down') return 'text-emerald-600';
    return 'text-on-surface-variant';
  }

  getPeakMonth(): string {
    const data = this.stats()?.tendanceMensuelle;
    if (!data || data.length === 0) return '—';
    const peak = data.reduce((max, m) => m.count > max.count ? m : max, data[0]);
    return peak.label + ' (' + this.formatNumber(peak.count) + ' sinistres)';
  }

  getMonthlyAverage(): string {
    const data = this.stats()?.tendanceMensuelle;
    if (!data || data.length === 0) return '0';
    const avg = data.reduce((sum, m) => sum + m.count, 0) / data.length;
    return this.formatNumber(Math.round(avg));
  }
}
