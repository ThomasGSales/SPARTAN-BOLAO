import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { RankingEntry } from '../models/ranking.model';

@Injectable({ providedIn: 'root' })
export class RankingService {
  private readonly http = inject(HttpClient);

  leaderboard(): Observable<RankingEntry[]> {
    return this.http.get<RankingEntry[]>('/api/ranking');
  }
}
