export type ChallengeStatus = "NOT_PLAYED" | "IN_PROGRESS" | "WON" | "LOST";

export type GameSessionStatus = "IN_PROGRESS" | "WON" | "LOST";

export type CellState = "HIDDEN" | "FLAGGED" | "REVEALED_SAFE" | "REVEALED_MINE";

export type SessionActionType = "REVEAL" | "TOGGLE_FLAG";

export type SessionActionResult =
  | "SAFE_REVEAL"
  | "SAFE_REVEAL_CASCADE"
  | "MINE_HIT"
  | "SESSION_WON"
  | "SESSION_LOST"
  | "FLAG_ADDED"
  | "FLAG_REMOVED";

export interface CurrentChallengeView {
  challengeId: string;
  challengeDate: string;
  timezone: string;
  windowStartAt: string;
  windowEndAt: string;
  rolloverAt: string;
  board: {
    rows: number;
    cols: number;
    mineCount: number;
  };
}

export interface CurrentUserChallengeStatusView {
  challengeId: string;
  challengeDate: string;
  status: ChallengeStatus;
  hasPlayedCurrentChallenge: boolean;
  hasActiveSession: boolean;
  activeSessionId?: string;
  canStartSession: boolean;
  canResumeSession: boolean;
  leaderboardEligible: boolean;
  remainingLives?: number;
  maxLives?: number;
  finishedOutcome?: "WON" | "LOST";
  sessionId?: string;
}

export interface CellView {
  row: number;
  col: number;
  state: CellState;
  adjacentMineCount: number | null;
}

export interface BoardSnapshotView {
  rows: number;
  cols: number;
  cells: CellView[][];
}

export interface GameSessionView {
  sessionId: string;
  challengeId: string;
  challengeDate: string;
  status: GameSessionStatus;
  startedAt: string;
  endedAt: string | null;
  durationMs?: number;
  lives: {
    maxLives: number;
    remainingLives: number;
  };
  performance: {
    errorCount: number;
    clickCount: number;
  };
  board: BoardSnapshotView;
}

export interface SessionActionResponse {
  action: {
    type: SessionActionType;
    row: number;
    col: number;
    result: SessionActionResult;
  };
  session: GameSessionView;
}

export interface DailyLeaderboardView {
  challengeId: string;
  challengeDate: string;
  entries: LeaderboardEntryView[];
}

export interface LeaderboardEntryView {
  position: number;
  displayName: string;
  durationMs: number;
  errorCount: number;
  clickCount: number;
  remainingLives: number;
  endedAt: string;
}
