import {
  Component,
  ElementRef,
  OnInit,
  computed,
  inject,
  signal,
  viewChild,
} from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { Subject, debounceTime, forkJoin, groupBy, interval, mergeMap } from 'rxjs';
import { MatchService } from '../../core/services/match.service';
import { GuessService } from '../../core/services/guess.service';
import { AuthService } from '../../core/services/auth.service';
import { HeaderComponent } from '../../shared/header';
import { Match, MatchPhase, PHASE_LABELS } from '../../core/models/match.model';
import { SaveState } from '../../core/models/guess.model';

interface LocalGuess {
  home: number | null;
  away: number | null;
}

interface PhaseGroup {
  phase: MatchPhase;
  label: string;
  matches: Match[];
}

/** Ordem canônica das fases. */
const PHASE_ORDER: MatchPhase[] = [
  'GROUP',
  'ROUND_OF_32',
  'ROUND_OF_16',
  'QUARTER',
  'SEMI',
  'THIRD_PLACE',
  'FINAL',
];

/** Polling "ao vivo" de placares/status/pontos. */
const POLL_MS = 20000;

type ViewMode = 'stepper' | 'all';

@Component({
  selector: 'spartan-matches',
  imports: [
    MatButtonModule,
    MatIconModule,
    MatProgressSpinnerModule,
    HeaderComponent,
  ],
  templateUrl: './matches.html',
  styleUrl: './matches.scss',
})
export class MatchesComponent implements OnInit {
  private readonly matchService = inject(MatchService);
  private readonly guessService = inject(GuessService);
  private readonly auth = inject(AuthService);

  protected readonly currentUser = this.auth.currentUser;

  protected readonly loading = signal(true);
  protected readonly error = signal<string | null>(null);
  protected readonly matches = signal<Match[]>([]);

  /** Palpites em memória: matchId -> { home, away }. */
  protected readonly guesses = signal<Record<string, LocalGuess>>({});
  /** Estado de salvamento por jogo. */
  protected readonly saveStates = signal<Record<string, SaveState>>({});
  /** Pontos ganhos (jogos encerrados). */
  protected readonly points = signal<Record<string, number>>({});

  /** Modo da tela: passo-a-passo (palpitar) ou lista geral (revisar). */
  protected readonly mode = signal<ViewMode>('stepper');
  /** Posição no stepper. */
  protected readonly index = signal(0);
  /** Relógio reativo (1s) para os contadores regressivos e o travamento ao vivo. */
  protected readonly nowMs = signal(Date.now());

  private readonly homeInput = viewChild<ElementRef<HTMLInputElement>>('homeInput');
  private readonly awayInput = viewChild<ElementRef<HTMLInputElement>>('awayInput');

  private readonly saveTrigger = new Subject<string>();

  constructor() {
    // Auto-save independente por jogo (debounce).
    this.saveTrigger
      .pipe(
        groupBy((id) => id),
        mergeMap((g) => g.pipe(debounceTime(600))),
        takeUntilDestroyed(),
      )
      .subscribe((id) => this.persist(id));

    // Relógio de 1s — alimenta contadores e o fechamento ao vivo dos palpites.
    interval(1000)
      .pipe(takeUntilDestroyed())
      .subscribe(() => this.nowMs.set(Date.now()));

    // Polling silencioso de placares/status/pontos (não toca em palpites/foco).
    interval(POLL_MS)
      .pipe(takeUntilDestroyed())
      .subscribe(() => this.poll());
  }

  // ---------------- Derivações ----------------

  private deadline(m: Match): number {
    return Date.parse(m.matchDatetime);
  }

  isFinished(m: Match): boolean {
    return m.homeScore != null && m.awayScore != null;
  }

  /** Jogo ainda palpitável: não travado, sem placar e antes do apito. */
  isOpen(m: Match): boolean {
    return !m.locked && !this.isFinished(m) && this.deadline(m) > this.nowMs();
  }

  /** Jogos abertos, do mais próximo ao mais distante. */
  protected readonly openMatches = computed(() =>
    this.matches()
      .filter((m) => this.isOpen(m))
      .sort((a, b) => this.deadline(a) - this.deadline(b)),
  );

  protected readonly total = computed(() => this.openMatches().length);

  /** Jogo atual do stepper (índice preso ao intervalo válido). */
  protected readonly current = computed<Match | null>(() => {
    const open = this.openMatches();
    if (open.length === 0) return null;
    const i = Math.min(this.index(), open.length - 1);
    return open[i];
  });

  /** Próximo jogo a começar (prazo mais urgente). */
  protected readonly nextMatch = computed<Match | null>(
    () => this.openMatches()[0] ?? null,
  );

  protected readonly filledCount = computed(
    () =>
      this.openMatches().filter((m) => {
        const g = this.guesses()[m.id];
        return g && g.home != null && g.away != null;
      }).length,
  );

  /** Posição "humana" (1-based) presa ao intervalo. */
  protected readonly position = computed(() =>
    this.total() === 0 ? 0 : Math.min(this.index(), this.total() - 1) + 1,
  );

  protected readonly isLastStep = computed(
    () => this.position() >= this.total(),
  );

  /** Todos os jogos agrupados por fase (modo "lista"). */
  protected readonly groups = computed<PhaseGroup[]>(() => {
    const byPhase = new Map<MatchPhase, Match[]>();
    for (const m of this.matches()) {
      (byPhase.get(m.phase) ?? byPhase.set(m.phase, []).get(m.phase)!).push(m);
    }
    return PHASE_ORDER.filter((p) => byPhase.has(p)).map((phase) => ({
      phase,
      label: PHASE_LABELS[phase],
      matches: byPhase
        .get(phase)!
        .slice()
        .sort((a, b) => this.deadline(a) - this.deadline(b)),
    }));
  });

  ngOnInit(): void {
    this.load();
  }

  // ---------------- Carga / polling ----------------

  load(): void {
    this.loading.set(true);
    this.error.set(null);
    forkJoin({
      matches: this.matchService.list(),
      guesses: this.guessService.mine(),
    }).subscribe({
      next: ({ matches, guesses }) => {
        this.matches.set(matches);
        const prefill: Record<string, LocalGuess> = {};
        const states: Record<string, SaveState> = {};
        const pts: Record<string, number> = {};
        for (const g of guesses) {
          prefill[g.matchId] = { home: g.homeScoreGuess, away: g.awayScoreGuess };
          states[g.matchId] = 'saved';
          pts[g.matchId] = g.pointsEarned;
        }
        this.guesses.set(prefill);
        this.saveStates.set(states);
        this.points.set(pts);
        this.loading.set(false);
        // Começa no primeiro jogo ainda sem palpite, para ir direto ao ponto.
        this.jumpToFirstUnanswered();
        this.focusHome();
      },
      error: () => {
        this.error.set('Não foi possível carregar os jogos. O backend está no ar?');
        this.loading.set(false);
      },
    });
  }

  private poll(): void {
    if (typeof document !== 'undefined' && document.hidden) return;
    forkJoin({
      matches: this.matchService.list(),
      guesses: this.guessService.mine(),
    }).subscribe({
      next: ({ matches, guesses }) => {
        this.matches.set(matches);
        const pts: Record<string, number> = {};
        for (const g of guesses) pts[g.matchId] = g.pointsEarned;
        this.points.set(pts);
      },
      error: () => {
        /* silencioso */
      },
    });
  }

  private jumpToFirstUnanswered(): void {
    const open = this.openMatches();
    const idx = open.findIndex((m) => {
      const g = this.guesses()[m.id];
      return !g || g.home == null || g.away == null;
    });
    this.index.set(idx >= 0 ? idx : 0);
  }

  // ---------------- Entrada de palpite ----------------

  guessValue(matchId: string, side: 'home' | 'away'): number | null {
    return this.guesses()[matchId]?.[side] ?? null;
  }

  onScore(matchId: string, side: 'home' | 'away', value: string): void {
    const parsed = value === '' ? null : Math.max(0, Math.min(99, Math.trunc(+value)));
    this.guesses.update((g) => {
      const current = g[matchId] ?? { home: null, away: null };
      return { ...g, [matchId]: { ...current, [side]: parsed } };
    });
    this.setState(matchId, 'idle');
    this.saveTrigger.next(matchId);
  }

  private persist(matchId: string): void {
    const g = this.guesses()[matchId];
    if (!g || g.home == null || g.away == null) return;
    this.setState(matchId, 'saving');
    this.guessService
      .save(matchId, { homeScoreGuess: g.home, awayScoreGuess: g.away })
      .subscribe({
        next: () => this.setState(matchId, 'saved'),
        error: () => this.setState(matchId, 'error'),
      });
  }

  private setState(matchId: string, state: SaveState): void {
    this.saveStates.update((s) => ({ ...s, [matchId]: state }));
  }

  saveState(matchId: string): SaveState {
    return this.saveStates()[matchId] ?? 'idle';
  }

  // ---------------- Navegação do stepper ----------------

  /** Enter no campo do mandante → vai para o visitante. */
  onHomeEnter(): void {
    this.focusAway();
  }

  /** Enter no campo do visitante → salva e avança. */
  onAwayEnter(): void {
    this.goNext();
  }

  goNext(): void {
    const m = this.current();
    if (m) this.persist(m.id);
    if (this.position() < this.total()) {
      this.index.update((i) => i + 1);
      this.focusHome();
    } else {
      // Último: conclui e leva para a revisão.
      this.mode.set('all');
    }
  }

  goPrev(): void {
    const m = this.current();
    if (m) this.persist(m.id);
    this.index.update((i) => Math.max(0, i - 1));
    this.focusHome();
  }

  goTo(i: number): void {
    this.index.set(i);
    this.focusHome();
  }

  setMode(mode: ViewMode): void {
    this.mode.set(mode);
    if (mode === 'stepper') {
      this.jumpToFirstUnanswered();
      this.focusHome();
    }
  }

  private focusHome(): void {
    setTimeout(() => {
      const el = this.homeInput()?.nativeElement;
      if (el) {
        el.focus();
        el.select();
      }
    });
  }

  private focusAway(): void {
    const el = this.awayInput()?.nativeElement;
    if (el) {
      el.focus();
      el.select();
    }
  }

  selectOnFocus(event: FocusEvent): void {
    (event.target as HTMLInputElement)?.select();
  }

  // ---------------- Contadores / formatação ----------------

  /** "1d 03:12:45" ou "03:12:45" até o apito; "Fechado" se passou. */
  countdown(m: Match | null): string {
    if (!m) return '';
    const ms = this.deadline(m) - this.nowMs();
    if (ms <= 0) return 'Fechado';
    const s = Math.floor(ms / 1000);
    const d = Math.floor(s / 86400);
    const h = Math.floor((s % 86400) / 3600);
    const mi = Math.floor((s % 3600) / 60);
    const se = s % 60;
    const p = (n: number) => String(n).padStart(2, '0');
    return d > 0 ? `${d}d ${p(h)}:${p(mi)}:${p(se)}` : `${p(h)}:${p(mi)}:${p(se)}`;
  }

  /** Faltando menos de 1h → urgência (cor vermelha pulsante). */
  isUrgent(m: Match | null): boolean {
    if (!m) return false;
    const ms = this.deadline(m) - this.nowMs();
    return ms > 0 && ms < 3600_000;
  }

  finalScore(m: Match): string | null {
    if (!this.isFinished(m)) return null;
    return `${m.homeScore} - ${m.awayScore}`;
  }

  pointsFor(matchId: string): number | null {
    return this.points()[matchId] ?? null;
  }

  formatKickoff(iso: string): string {
    return new Date(iso).toLocaleString('pt-BR', {
      weekday: 'short',
      day: '2-digit',
      month: 'short',
      hour: '2-digit',
      minute: '2-digit',
    });
  }

  trackPhase = (_: number, g: PhaseGroup) => g.phase;
  trackMatch = (_: number, m: Match) => m.id;
}
