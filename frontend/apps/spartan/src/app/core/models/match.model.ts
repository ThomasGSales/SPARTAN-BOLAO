export type MatchPhase =
  | 'GROUP'
  | 'ROUND_OF_32'
  | 'ROUND_OF_16'
  | 'QUARTER'
  | 'SEMI'
  | 'THIRD_PLACE'
  | 'FINAL';

export type MatchStatus = 'SCHEDULED' | 'LIVE' | 'FINISHED' | 'CANCELLED';

export interface Team {
  id: string;
  name: string;
  code: string;
  flagUrl: string | null;
}

export interface Match {
  id: string;
  phase: MatchPhase;
  groupLabel: string | null;
  matchDatetime: string; // ISO 8601
  status: MatchStatus;
  locked: boolean;
  homeTeam: Team;
  awayTeam: Team;
  homeScore: number | null;
  awayScore: number | null;
}

/** Rótulos amigáveis das fases (para os cabeçalhos da UI). */
export const PHASE_LABELS: Record<MatchPhase, string> = {
  GROUP: 'Fase de Grupos',
  ROUND_OF_32: '16-avos de Final',
  ROUND_OF_16: 'Oitavas de Final',
  QUARTER: 'Quartas de Final',
  SEMI: 'Semifinais',
  THIRD_PLACE: 'Disputa de 3º Lugar',
  FINAL: 'Final',
};
