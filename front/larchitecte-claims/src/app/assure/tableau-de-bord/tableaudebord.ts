import { Component, OnInit, signal, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { ClaimService, ClaimResponse } from '../../core/claim.service';
import { AuthService } from '../../core/auth.service';

@Component({
  selector: 'app-tableau-de-bord',
  standalone: true,
  imports: [CommonModule, RouterLink],
  templateUrl: './tableaudebord.html'
})
export class TableauDeBordAssure implements OnInit {
  claims = signal<ClaimResponse[]>([]);
  isLoading = signal(true);

  readonly userName = computed(() => this.authService.userName());

  readonly enCoursCount = computed(() =>
    this.claims().filter(c => c.statut === 'EN_COURS').length
  );
  readonly enRevisionCount = computed(() =>
    this.claims().filter(c => c.statut === 'EN_REVISION' || c.statut === 'EXPERTISE').length
  );
  readonly resolusCount = computed(() =>
    this.claims().filter(c => c.statut === 'VALIDE' || c.statut === 'CLOTURE').length
  );
  readonly totalCount = computed(() => this.claims().length);

  readonly recentClaims = computed(() =>
    [...this.claims()]
      .sort((a, b) => new Date(b.createdAt).getTime() - new Date(a.createdAt).getTime())
      .slice(0, 5)
  );

  typeLabels: Record<string, string> = {
    'water': 'Dégât des Eaux',
    'fire': 'Incendie',
    'theft': 'Vol avec Effraction',
    'auto': 'Sinistre Auto',
    'natural': 'Catastrophe naturelle'
  };

  statutLabels: Record<string, string> = {
    'EN_COURS': 'En cours',
    'EN_REVISION': 'En révision',
    'EXPERTISE': 'Expertise',
    'VALIDE': 'Validé',
    'REFUSE': 'Refusé',
    'CLOTURE': 'Clôturé'
  };

  constructor(
    private claimService: ClaimService,
    private authService: AuthService
  ) {}

  ngOnInit(): void {
    this.loadClaims();
  }

  loadClaims(): void {
    this.isLoading.set(true);
    this.claimService.getMyClaims().subscribe({
      next: (data) => {
        this.claims.set(data);
        this.isLoading.set(false);
      },
      error: () => {
        this.isLoading.set(false);
      }
    });
  }

  getTypeLabel(type: string): string {
    return this.typeLabels[type] || type;
  }

  getStatutLabel(statut: string): string {
    return this.statutLabels[statut] || statut;
  }

  getStatutClass(statut: string): string {
    switch (statut) {
      case 'EN_COURS': return 'bg-tertiary-fixed text-on-tertiary-fixed-variant';
      case 'EN_REVISION': case 'EXPERTISE': return 'bg-amber-100 text-amber-700';
      case 'VALIDE': case 'CLOTURE': return 'bg-emerald-100 text-emerald-700';
      case 'REFUSE': return 'bg-error-container text-on-error-container';
      default: return 'bg-gray-100 text-gray-700';
    }
  }

  formatDate(dateStr: string): string {
    if (!dateStr) return '—';
    const date = new Date(dateStr);
    return date.toLocaleDateString('fr-FR', { day: 'numeric', month: 'short', year: 'numeric' });
  }

  formatClaimId(id: string): string {
    if (!id) return '';
    return '#SIN-' + id.substring(0, 6).toUpperCase();
  }
}
