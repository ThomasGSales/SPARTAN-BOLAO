export interface GuessResponse {
  id: string;
  matchId: string;
  homeScoreGuess: number;
  awayScoreGuess: number;
  pointsEarned: number;
  updatedAt: string;
}

export interface GuessPayload {
  homeScoreGuess: number;
  awayScoreGuess: number;
}

export interface BulkGuessItem extends GuessPayload {
  matchId: string;
}

/** Estado de salvamento de um palpite na UI. */
export type SaveState = 'idle' | 'saving' | 'saved' | 'error';
