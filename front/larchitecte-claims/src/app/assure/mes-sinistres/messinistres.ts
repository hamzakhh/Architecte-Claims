import { Component, OnInit, inject, signal, computed, DestroyRef } from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import { CommonModule } from '@angular/common';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { ClaimService, ClaimResponse } from '../../core/claim.service';

@Component({
  selector: 'app-mes-sinistres',
  standalone: true,
  imports: [RouterLink, CommonModule],
  templateUrl: './messinistres.html'
})
export class MesSinistres implements OnInit {
  private destroyRef = inject(DestroyRef);
  private router = inject(Router);
  private claimService = inject(ClaimService);

  claims = signal<ClaimResponse[]>([]);
  loading = signal(true);
  error = signal<string | null>(null);
  selectedFilter = signal<string>('tous');
  searchTerm = signal<string>('');

  typeLabels: Record<string, string> = {
    'water': 'Dégât des Eaux',
    'fire': 'Incendie',
    'theft': 'Vol',
    'auto': 'Auto',
    'natural': 'Catastrophe'
  };

  ngOnInit(): void {
    this.loadClaims();
  }

  private loadClaims(): void {
    this.loading.set(true);
    this.error.set(null);

    this.claimService.getMyClaims()
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (claims) => {
          this.claims.set(claims);
          this.loading.set(false);
        },
        error: (err) => {
          console.error('Error loading claims:', err);
          this.error.set('Impossible de charger vos sinistres');
          this.loading.set(false);
        }
      });
  }

  getStatusLabel(status: string): string {
    switch (status) {
      case 'EN_COURS': return 'En cours';
      case 'EN_REVISION': return 'En révision';
      case 'EXPERTISE': return 'Expertise';
      case 'VALIDE': return 'Validé';
      case 'REFUSE': return 'Refusé';
      case 'CLOTURE': return 'Clôturé';
      default: return status;
    }
  }

  getStatusColor(status: string): string {
    switch (status) {
      case 'EN_COURS': return 'bg-amber-100 text-amber-700';
      case 'EN_REVISION': return 'bg-blue-100 text-blue-700';
      case 'EXPERTISE': return 'bg-purple-100 text-purple-700';
      case 'VALIDE': return 'bg-emerald-100 text-emerald-700';
      case 'REFUSE': return 'bg-red-100 text-red-700';
      case 'CLOTURE': return 'bg-slate-100 text-slate-600';
      default: return 'bg-slate-100 text-slate-600';
    }
  }

  getStatusIcon(status: string): string {
    switch (status) {
      case 'EN_COURS': return 'hourglass_empty';
      case 'EN_REVISION': return 'visibility';
      case 'EXPERTISE': return 'search';
      case 'VALIDE': return 'check_circle';
      case 'REFUSE': return 'cancel';
      case 'CLOTURE': return 'lock';
      default: return 'help';
    }
  }

  getTypeLabel(type: string): string {
    return this.typeLabels[type] || type;
  }

  formatDate(dateString: string): string {
    if (!dateString) return 'N/A';
    const date = new Date(dateString);
    return date.toLocaleDateString('fr-FR', {
      day: 'numeric',
      month: 'short',
      year: 'numeric'
    });
  }

  navigateToDetail(claimId: string): void {
    this.router.navigate(['/assure/detail-sinistre', claimId]);
  }

  setFilter(filter: string): void {
    this.selectedFilter.set(filter);
  }

  onSearch(event: Event): void {
    const value = (event.target as HTMLInputElement).value;
    this.searchTerm.set(value);
  }

  totalClaims = computed(() => this.claims().length);

  enCoursCount = computed(() =>
    this.claims().filter(c => c.statut === 'EN_COURS' || c.statut === 'EN_REVISION' || c.statut === 'EXPERTISE').length
  );

  validesCount = computed(() =>
    this.claims().filter(c => c.statut === 'VALIDE').length
  );

  cloturesCount = computed(() =>
    this.claims().filter(c => c.statut === 'CLOTURE' || c.statut === 'REFUSE').length
  );

  filteredClaims = computed(() => {
    const filter = this.selectedFilter();
    const search = this.searchTerm().toLowerCase();
    let result = this.claims();

    if (filter === 'en_cours') {
      result = result.filter(c => c.statut === 'EN_COURS' || c.statut === 'EN_REVISION' || c.statut === 'EXPERTISE');
    } else if (filter === 'valides') {
      result = result.filter(c => c.statut === 'VALIDE');
    } else if (filter === 'clotures') {
      result = result.filter(c => c.statut === 'CLOTURE' || c.statut === 'REFUSE');
    }

    if (search) {
      result = result.filter(c =>
        c.id?.toLowerCase().includes(search) ||
        c.type?.toLowerCase().includes(search) ||
        c.description?.toLowerCase().includes(search) ||
        this.getTypeLabel(c.type).toLowerCase().includes(search) ||
        this.getStatusLabel(c.statut).toLowerCase().includes(search)
      );
    }

    return result;
  });
}
