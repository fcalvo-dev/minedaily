import type {
  CurrentChallengeView,
  CurrentUserChallengeStatusView,
  DailyLeaderboardView,
  GameSessionView,
  SessionActionResponse
} from "../types/minedaily";

interface ApiErrorPayload {
  code?: string;
  message?: string;
  status?: number;
}

export class ApiClientError extends Error {
  readonly status: number;
  readonly code?: string;

  constructor(message: string, status: number, code?: string) {
    super(message);
    this.name = "ApiClientError";
    this.status = status;
    this.code = code;
  }
}

const devUsername = import.meta.env.VITE_MINEDAILY_USERNAME ?? (import.meta.env.DEV ? "tester" : "");
const devPassword = import.meta.env.VITE_MINEDAILY_PASSWORD ?? (import.meta.env.DEV ? "tester123" : "");

function buildHeaders(body?: BodyInit | null): Headers {
  const headers = new Headers();

  headers.set("Accept", "application/json");

  if (body !== undefined && body !== null) {
    headers.set("Content-Type", "application/json");
  }

  if (devUsername !== "" && devPassword !== "") {
    headers.set("Authorization", `Basic ${btoa(`${devUsername}:${devPassword}`)}`);
  }

  return headers;
}

async function readErrorPayload(response: Response): Promise<ApiErrorPayload | undefined> {
  const contentType = response.headers.get("content-type");

  if (!contentType?.includes("application/json")) {
    return undefined;
  }

  try {
    return (await response.json()) as ApiErrorPayload;
  } catch {
    return undefined;
  }
}

async function requestJson<T>(path: string, init: RequestInit = {}): Promise<T> {
  const response = await fetch(path, {
    ...init,
    headers: buildHeaders(init.body)
  });

  if (response.status === 204) {
    return null as T;
  }

  if (!response.ok) {
    const payload = await readErrorPayload(response);
    throw new ApiClientError(
      payload?.message ?? `Request failed with HTTP ${response.status}`,
      payload?.status ?? response.status,
      payload?.code
    );
  }

  return (await response.json()) as T;
}

export function getCurrentChallenge(): Promise<CurrentChallengeView> {
  return requestJson<CurrentChallengeView>("/api/challenges/current");
}

export function getCurrentChallengeStatus(): Promise<CurrentUserChallengeStatusView> {
  return requestJson<CurrentUserChallengeStatusView>("/api/challenges/current/status");
}

export function createOrReuseCurrentSession(): Promise<GameSessionView> {
  return requestJson<GameSessionView>("/api/challenges/current/sessions", {
    method: "POST"
  });
}

export async function getActiveCurrentSession(): Promise<GameSessionView | null> {
  return requestJson<GameSessionView | null>("/api/challenges/current/sessions/active");
}

export function getSession(sessionId: string): Promise<GameSessionView> {
  return requestJson<GameSessionView>(`/api/sessions/${sessionId}`);
}

export function revealCell(
  sessionId: string,
  row: number,
  col: number
): Promise<SessionActionResponse> {
  return requestJson<SessionActionResponse>(`/api/sessions/${sessionId}/actions/reveal`, {
    method: "POST",
    body: JSON.stringify({ row, col })
  });
}

export function toggleFlag(
  sessionId: string,
  row: number,
  col: number
): Promise<SessionActionResponse> {
  return requestJson<SessionActionResponse>(`/api/sessions/${sessionId}/actions/toggle-flag`, {
    method: "POST",
    body: JSON.stringify({ row, col })
  });
}

export function getCurrentLeaderboard(): Promise<DailyLeaderboardView> {
  return requestJson<DailyLeaderboardView>("/api/challenges/current/leaderboard");
}
