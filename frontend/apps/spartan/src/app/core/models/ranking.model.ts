export interface RankingEntry {
  position: number;
  userId: string;
  name: string;
  avatarUrl: string | null;
  totalPoints: number;
  exactHits: number;
  totalGuesses: number;
  isMe: boolean;
}
