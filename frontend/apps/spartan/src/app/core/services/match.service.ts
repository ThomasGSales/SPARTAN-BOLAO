import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, map } from 'rxjs';
import { Match, MatchPhase } from '../models/match.model';

@Injectable({ providedIn: 'root' })
export class MatchService {
  private readonly http = inject(HttpClient);
  // Proxy do dev-server (proxy.conf.json) ou Nginx encaminham /api para o backend.
  private readonly baseUrl = '/api/matches';

  /** Lista os jogos; se [phase] vier, filtra por fase. */
  list(phase?: MatchPhase): Observable<Match[]> {
    const url = phase ? `${this.baseUrl}?phase=${phase}` : this.baseUrl;
    // O backend omite campos nulos (jackson non_null), então homeScore/awayScore
    // de jogos abertos chegam como `undefined`. Normalizamos para `null` — senão
    // checagens `=== null` falham e a UI trata jogo aberto como encerrado.
    return this.http.get<Match[]>(url).pipe(
      map((matches) =>
        matches.map((m) => ({
          ...m,
          homeScore: m.homeScore ?? null,
          awayScore: m.awayScore ?? null,
        })),
      ),
    );
  }
}
