import { Component, inject, signal, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { AnalyseIAService, AnalyseIAStats, AnalyseIAResponse } from '../../core/analyse-ia.service';

@Component({
  selector: 'app-monitoring-ia',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './monitoringia.html'
})
export class MonitoringIA implements OnInit {
  private analyseIAService = inject(AnalyseIAService);

  stats = signal<AnalyseIAStats | null>(null);
  analyses = signal<AnalyseIAResponse[]>([]);
  loading = signal(true);
  error = signal('');

  ngOnInit(): void {
    this.loadData();
  }

  loadData(): void {
    this.loading.set(true);
    this.error.set('');

    this.analyseIAService.getStats().subscribe({
      next: (data) => {
        this.stats.set(data);
        this.loading.set(false);
      },
      error: () => {
        this.error.set('Erreur lors du chargement des statistiques');
        this.loading.set(false);
      }
    });

    this.analyseIAService.getAllAnalyses().subscribe({
      next: (data) => this.analyses.set(data),
      error: () => {}
    });
  }

  getSeveriteClass(severite: string | null): string {
    switch (severite) {
      case 'CRITIQUE': return 'bg-red-100 text-red-800';
      case 'ELEVEE': return 'bg-orange-100 text-orange-800';
      case 'MODEREE': return 'bg-amber-100 text-amber-800';
      case 'FAIBLE': return 'bg-green-100 text-green-800';
      default: return 'bg-slate-100 text-slate-800';
    }
  }

  formatDate(dateStr: string | null): string {
    if (!dateStr) return '—';
    const date = new Date(dateStr);
    return date.toLocaleDateString('fr-FR', { day: 'numeric', month: 'short', year: 'numeric', hour: '2-digit', minute: '2-digit' });
  }
}
