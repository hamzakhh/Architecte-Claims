import { Routes } from '@angular/router';
import { authGuard } from './core/auth.guard';

export const routes: Routes = [
  { path: '', redirectTo: 'auth/connexion', pathMatch: 'full' },
  {
    path: 'auth',
    children: [
      { path: 'connexion', loadComponent: () => import('./auth/connexion/connexion').then(m => m.Connexion) },
      { path: 'inscription', loadComponent: () => import('./auth/inscription/inscription').then(m => m.Inscription) }
    ]
  },
  {
    path: 'assure',
    canActivate: [authGuard],
    data: { roles: ['ASSURE'] },
    loadComponent: () => import('./assure/assure-layout/assurelayout').then(m => m.AssureLayout),
    children: [
      { path: '', redirectTo: 'tableau-de-bord', pathMatch: 'full' },
      { path: 'tableau-de-bord', loadComponent: () => import('./assure/tableau-de-bord/tableaudebord').then(m => m.TableauDeBordAssure) },
      { path: 'mes-sinistres', loadComponent: () => import('./assure/mes-sinistres/messinistres').then(m => m.MesSinistres) },
      { path: 'declarer-sinistre', loadComponent: () => import('./assure/declarer-sinistre/declarersinistre').then(m => m.DeclarerSinistre) },
      { path: 'detail-sinistre/:id', loadComponent: () => import('./assure/detail-sinistre/detailsinistre').then(m => m.DetailSinistre) },
      { path: 'remboursements', loadComponent: () => import('./assure/remboursements/remboursements').then(m => m.RemboursementsAssure) },
      { path: 'messagerie', loadComponent: () => import('./assure/messagerie/messagerie').then(m => m.MessagerieAssure) },
      { path: 'faq', loadComponent: () => import('./assure/faq-aide/faqaide').then(m => m.FaqAide) },
      { path: 'support', loadComponent: () => import('./assure/support/support').then(m => m.SupportAssure) },
      { path: 'profil', loadComponent: () => import('./assure/profil/profil').then(m => m.ProfilAssure) },
    ]
  },
  {
    path: 'gestionnaire',
    canActivate: [authGuard],
    data: { roles: ['GESTIONNAIRE'] },
    loadComponent: () => import('./gestionnaire/gestionnaire-layout/gestionnairelayout').then(m => m.GestionnaireLayout),
    children: [
      { path: '', redirectTo: 'tableau-de-bord', pathMatch: 'full' },
      { path: 'tableau-de-bord', loadComponent: () => import('./gestionnaire/tableau-de-bord/tableaudebord').then(m => m.TableauDeBordGestionnaire) },
      { path: 'dossiers-assignes', loadComponent: () => import('./gestionnaire/dossiers-assignes/dossiersassignes').then(m => m.DossiersAssignes) },
      { path: 'tous-sinistres', loadComponent: () => import('./gestionnaire/tous-sinistres/toussinistres').then(m => m.TousSinistres) },
      { path: 'messagerie', loadComponent: () => import('./gestionnaire/messagerie/messagerie').then(m => m.MessagerieGestionnaire) },
      { path: 'remboursements', loadComponent: () => import('./gestionnaire/remboursements/remboursements').then(m => m.RemboursementsGestionnaire) },
      { path: 'support', loadComponent: () => import('./gestionnaire/support/support').then(m => m.SupportGestionnaire) },
      { path: 'analyse-ia', loadComponent: () => import('./gestionnaire/analyse-ia/analyseia').then(m => m.AnalyseIA) },
      { path: 'approbation', loadComponent: () => import('./gestionnaire/approbation/approbation').then(m => m.Approbation) },
      { path: 'rapports', loadComponent: () => import('./gestionnaire/rapports/rapports').then(m => m.RapportsGestionnaire) },
      { path: 'signalement-fraude', loadComponent: () => import('./gestionnaire/signalement-fraude/signalementfraude').then(m => m.SignalementFraude) },
      { path: 'profil', loadComponent: () => import('./gestionnaire/profil/profil').then(m => m.ProfilGestionnaire) }
    ]
  },
  {
    path: 'expert',
    canActivate: [authGuard],
    data: { roles: ['EXPERT'] },
    loadComponent: () => import('./expert/expert-layout/expertlayout').then(m => m.ExpertLayout),
    children: [
      { path: '', redirectTo: 'tableau-de-bord', pathMatch: 'full' },
      { path: 'tableau-de-bord', loadComponent: () => import('./expert/tableau-de-bord/tableaudebord').then(m => m.TableauDeBordExpert) },
      { path: 'dossiers', loadComponent: () => import('./expert/liste-dossiers/listedossiers').then(m => m.ListeDossiers) },
      { path: 'rapport-expertise', loadComponent: () => import('./expert/rapport-expertise/rapportexpertise').then(m => m.RapportExpertise) },
      { path: 'messagerie', loadComponent: () => import('./expert/messagerie/messagerie').then(m => m.MessagerieExpert) },
      { path: 'profil', loadComponent: () => import('./expert/profil/profil').then(m => m.ProfilExpert) }
    ]
  },
  {
    path: 'admin',
    canActivate: [authGuard],
    data: { roles: ['ADMIN'] },
    loadComponent: () => import('./admin/admin-layout/adminlayout').then(m => m.AdminLayout),
    children: [
      { path: '', redirectTo: 'tableau-de-bord', pathMatch: 'full' },
      { path: 'tableau-de-bord', loadComponent: () => import('./admin/tableau-de-bord/tableaudebord').then(m => m.TableauDeBordAdmin) },
      { path: 'utilisateurs', loadComponent: () => import('./admin/gestion-utilisateurs/gestionutilisateurs').then(m => m.GestionUtilisateurs) },
      { path: 'experts', loadComponent: () => import('./admin/gestion-experts/gestionexperts').then(m => m.GestionExperts) },
      { path: 'rapports', loadComponent: () => import('./admin/rapports-analytics/rapportsanalytics').then(m => m.RapportsAnalytics) },
      { path: 'monitoring-ia', loadComponent: () => import('./admin/monitoring-ia/monitoringia').then(m => m.MonitoringIA) },
      { path: 'gestion-technique', loadComponent: () => import('./admin/gestion-technique/gestiontechnique').then(m => m.GestionTechnique) },
      { path: 'surveillance-fraude', loadComponent: () => import('./admin/surveillance-fraude/surveillancefraude').then(m => m.SurveillanceFraude) }
    ]
  }
];
