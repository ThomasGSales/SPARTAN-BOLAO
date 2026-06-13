import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import {
  BulkGuessItem,
  GuessPayload,
  GuessResponse,
} from '../models/guess.model';

@Injectable({ providedIn: 'root' })
export class GuessService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = '/api/guesses';

  /** Palpites do usuário atual. */
  mine(): Observable<GuessResponse[]> {
    return this.http.get<GuessResponse[]>(this.baseUrl);
  }

  /** Cria/atualiza um palpite (auto-save). */
  save(matchId: string, payload: GuessPayload): Observable<GuessResponse> {
    return this.http.put<GuessResponse>(`${this.baseUrl}/${matchId}`, payload);
  }

  /** Salva vários de uma vez ("Salvar Tudo"). */
  saveBulk(guesses: BulkGuessItem[]): Observable<GuessResponse[]> {
    return this.http.post<GuessResponse[]>(`${this.baseUrl}/bulk`, { guesses });
  }
}
