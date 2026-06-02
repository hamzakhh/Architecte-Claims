export function getStatutLabel(statut: string): string {
  const labels: Record<string, string> = {
    'EN_COURS': 'En cours',
    'EN_REVISION': 'En révision',
    'EXPERTISE': 'Expertise',
    'VALIDE': 'Validé',
    'REFUSE': 'Refusé',
    'INDEMNISATION_PROPOSEE': 'Indemnisation proposée',
    'INDEMNISATION_ACCEPTEE': 'Indemnisation acceptée',
    'PAIEMENT_EN_COURS': 'Paiement en cours',
    'RECOURS': 'Recours / Litige',
    'CLOTURE': 'Clôturé',
    'ARCHIVE': 'Archivé'
  };
  return labels[statut] || statut;
}

export function getStatutClass(statut: string): string {
  const classes: Record<string, string> = {
    'EN_COURS': 'bg-tertiary-fixed text-on-tertiary-fixed-variant',
    'EN_REVISION': 'bg-amber-100 text-amber-700',
    'EXPERTISE': 'bg-blue-100 text-blue-700',
    'VALIDE': 'bg-emerald-100 text-emerald-700',
    'REFUSE': 'bg-error-container text-on-error-container',
    'INDEMNISATION_PROPOSEE': 'bg-purple-100 text-purple-700',
    'INDEMNISATION_ACCEPTEE': 'bg-violet-100 text-violet-700',
    'PAIEMENT_EN_COURS': 'bg-cyan-100 text-cyan-700',
    'RECOURS': 'bg-orange-100 text-orange-700',
    'CLOTURE': 'bg-slate-100 text-slate-600',
    'ARCHIVE': 'bg-gray-100 text-gray-500'
  };
  return classes[statut] || 'bg-slate-100 text-slate-600';
}

export function getGraviteLabel(gravite: string | null): string {
  if (!gravite) return '—';
  const labels: Record<string, string> = {
    'MINEURE': 'Mineure',
    'MODEREE': 'Modérée',
    'MAJEURE': 'Majeure',
    'CRITIQUE': 'Critique'
  };
  return labels[gravite] || gravite;
}

export function getGraviteClass(gravite: string | null): string {
  if (!gravite) return 'bg-slate-100 text-slate-600';
  const classes: Record<string, string> = {
    'MINEURE': 'bg-green-100 text-green-700',
    'MODEREE': 'bg-amber-100 text-amber-700',
    'MAJEURE': 'bg-orange-100 text-orange-700',
    'CRITIQUE': 'bg-red-100 text-red-700'
  };
  return classes[gravite] || 'bg-slate-100 text-slate-600';
}

export function getExpertiseStatutLabel(statut: string): string {
  const labels: Record<string, string> = {
    'EN_ATTENTE': 'En attente',
    'EN_COURS': 'En cours',
    'SOUMISE': 'Soumise',
    'VALIDEE': 'Validée',
    'REFUSEE': 'Refusée'
  };
  return labels[statut] || statut;
}

export function getExpertiseStatutClass(statut: string): string {
  const classes: Record<string, string> = {
    'EN_ATTENTE': 'bg-amber-100 text-amber-700',
    'EN_COURS': 'bg-blue-100 text-blue-700',
    'SOUMISE': 'bg-purple-100 text-purple-700',
    'VALIDEE': 'bg-emerald-100 text-emerald-700',
    'REFUSEE': 'bg-red-100 text-red-700'
  };
  return classes[statut] || 'bg-slate-100 text-slate-600';
}

export function getTypeLabel(type: string): string {
  const labels: Record<string, string> = {
    'water': 'Dégât des Eaux',
    'fire': 'Incendie',
    'theft': 'Vol avec Effraction',
    'auto': 'Accident Auto',
    'natural': 'Catastrophe Naturelle'
  };
  return labels[type] || type;
}

export function formatDate(dateStr: string | null | undefined): string {
  if (!dateStr) return '—';
  try {
    const date = new Date(dateStr);
    return date.toLocaleDateString('fr-FR', { day: 'numeric', month: 'short', year: 'numeric' });
  } catch {
    return dateStr;
  }
}

export function formatDateTime(dateStr: string | null | undefined): string {
  if (!dateStr) return '—';
  try {
    const date = new Date(dateStr);
    return date.toLocaleDateString('fr-FR', { day: 'numeric', month: 'short', year: 'numeric', hour: '2-digit', minute: '2-digit' });
  } catch {
    return dateStr;
  }
}

export function getSpecialiteLabel(specialite: string | null | undefined): string {
  if (!specialite) return '—';
  const labels: Record<string, string> = {
    'accident': 'Accident',
    'incendie': 'Incendie',
    'vol': 'Vol',
    'degat_eaux': 'Dégât des eaux',
    'catastrophe': 'Catastrophe',
    'autre': 'Autre'
  };
  return labels[specialite] || specialite;
}
