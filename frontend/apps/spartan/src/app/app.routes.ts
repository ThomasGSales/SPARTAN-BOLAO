import { Route } from '@angular/router';
import { authGuard, guestGuard } from './core/guards/auth.guard';

export const appRoutes: Route[] = [
  { path: '', pathMatch: 'full', redirectTo: 'jogos' },
  {
    path: 'login',
    canActivate: [guestGuard],
    loadComponent: () =>
      import('./features/auth/login').then((m) => m.LoginComponent),
    title: 'Entrar · SPARTAN',
  },
  {
    path: 'jogos',
    canActivate: [authGuard],
    loadComponent: () =>
      import('./features/matches/matches').then((m) => m.MatchesComponent),
    title: 'Jogos · SPARTAN',
  },
  {
    path: 'ranking',
    canActivate: [authGuard],
    loadComponent: () =>
      import('./features/ranking/ranking').then((m) => m.RankingComponent),
    title: 'Ranking · SPARTAN',
  },
  { path: '**', redirectTo: 'jogos' },
];
