import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { NgClass } from '@angular/common';
import { interval } from 'rxjs';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { RankingService } from '../../core/services/ranking.service';
import { RankingEntry } from '../../core/models/ranking.model';
import { HeaderComponent } from '../../shared/header';

/** Intervalo do polling "ao vivo" do ranking. */
const POLL_MS = 15000;

@Component({
  selector: 'spartan-ranking',
  imports: [
    NgClass,
    MatButtonModule,
    MatIconModule,
    MatProgressSpinnerModule,
    HeaderComponent,
  ],
  templateUrl: './ranking.html',
  styleUrl: './ranking.scss',
})
export class RankingComponent implements OnInit {
  private readonly rankingService = inject(RankingService);

  protected readonly loading = signal(true);
  protected readonly error = signal<string | null>(null);
  protected readonly entries = signal<RankingEntry[]>([]);

  /** Top 3 para o pódio. */
  protected readonly podium = computed(() => this.entries().slice(0, 3));
  /** Do 4º em diante, para a lista. */
  protected readonly rest = computed(() => this.entries().slice(3));

  constructor() {
    // Polling "ao vivo" do ranking (silencioso). Pausa com a aba oculta.
    interval(POLL_MS)
      .pipe(takeUntilDestroyed())
      .subscribe(() => {
        if (typeof document !== 'undefined' && document.hidden) return;
        this.fetch(false);
      });
  }

  ngOnInit(): void {
    this.fetch(true);
  }

  load(): void {
    this.fetch(true);
  }

  /** [showSpinner] true só na carga inicial / retry; false no polling. */
  private fetch(showSpinner: boolean): void {
    if (showSpinner) {
      this.loading.set(true);
      this.error.set(null);
    }
    this.rankingService.leaderboard().subscribe({
      next: (data) => {
        this.entries.set(data);
        this.loading.set(false);
      },
      error: () => {
        if (showSpinner) {
          this.error.set('Não foi possível carregar o ranking.');
          this.loading.set(false);
        }
        /* no polling, erro é silencioso */
      },
    });
  }

  medal(position: number): string {
    return position === 1 ? '🥇' : position === 2 ? '🥈' : '🥉';
  }

  trackEntry = (_: number, e: RankingEntry) => e.userId;
}
