import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { Match } from '../models/match.model';

@Injectable({ providedIn: 'root' })
export class AdminService {
  private readonly http = inject(HttpClient);

  /** Lança o placar oficial de um jogo (somente ADMIN). */
  setResult(matchId: string, homeScore: number, awayScore: number): Observable<Match> {
    return this.http.patch<Match>(`/api/admin/matches/${matchId}/result`, {
      homeScore,
      awayScore,
    });
  }
}
