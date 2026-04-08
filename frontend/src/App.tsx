import { useEffect, useState } from "react";
import {
  ApiClientError,
  createOrReuseCurrentSession,
  getActiveCurrentSession,
  getCurrentChallenge,
  getCurrentChallengeStatus,
  getCurrentLeaderboard,
  getSession,
  revealCell,
  toggleFlag
} from "./api/minedailyApi";
import { Board } from "./components/Board";
import { LeaderboardPanel } from "./components/LeaderboardPanel";
import { SessionHud } from "./components/SessionHud";
import type {
  CurrentChallengeView,
  CurrentUserChallengeStatusView,
  DailyLeaderboardView,
  GameSessionView,
  SessionActionResult
} from "./types/minedaily";

type LoadState = "loading" | "ready" | "error";

function errorMessage(error: unknown): string {
  if (error instanceof ApiClientError) {
    return error.message;
  }

  if (error instanceof Error) {
    return error.message;
  }

  return "Ocurrio un error inesperado.";
}

function actionResultText(result: SessionActionResult): string {
  switch (result) {
    case "SAFE_REVEAL":
      return "Celda revelada.";
    case "SAFE_REVEAL_CASCADE":
      return "Zona abierta.";
    case "MINE_HIT":
      return "Pisaste una mina. Perdiste una vida.";
    case "SESSION_WON":
      return "Ganaste el challenge.";
    case "SESSION_LOST":
      return "Te quedaste sin vidas.";
    case "FLAG_ADDED":
      return "Bandera marcada.";
    case "FLAG_REMOVED":
      return "Bandera retirada.";
    default:
      return "Accion aplicada.";
  }
}

function statusSummary(status: CurrentUserChallengeStatusView): string {
  if (status.status === "NOT_PLAYED") {
    return "Todavia no jugaste el challenge vigente.";
  }

  if (status.status === "IN_PROGRESS") {
    return "Tenes una sesion en curso.";
  }

  if (status.status === "WON") {
    return "Ya ganaste tu primera sesion del dia.";
  }

  return "Ya terminaste tu primera sesion del dia.";
}

function App() {
  const [loadState, setLoadState] = useState<LoadState>("loading");
  const [challenge, setChallenge] = useState<CurrentChallengeView | null>(null);
  const [status, setStatus] = useState<CurrentUserChallengeStatusView | null>(null);
  const [session, setSession] = useState<GameSessionView | null>(null);
  const [leaderboard, setLeaderboard] = useState<DailyLeaderboardView | null>(null);
  const [pageError, setPageError] = useState<string | null>(null);
  const [actionError, setActionError] = useState<string | null>(null);
  const [leaderboardError, setLeaderboardError] = useState<string | null>(null);
  const [feedback, setFeedback] = useState<string | null>(null);
  const [sessionBusy, setSessionBusy] = useState(false);
  const [actionPending, setActionPending] = useState(false);
  const [leaderboardLoading, setLeaderboardLoading] = useState(true);

  const refreshStatus = async () => {
    const nextStatus = await getCurrentChallengeStatus();
    setStatus(nextStatus);
    return nextStatus;
  };

  const refreshLeaderboard = async () => {
    setLeaderboardLoading(true);
    setLeaderboardError(null);

    try {
      setLeaderboard(await getCurrentLeaderboard());
    } catch (error) {
      setLeaderboardError(errorMessage(error));
    } finally {
      setLeaderboardLoading(false);
    }
  };

  const loadInitialState = async () => {
    setLoadState("loading");
    setPageError(null);
    setActionError(null);
    setFeedback(null);
    setLeaderboardLoading(true);

    try {
      const [currentChallenge, currentStatus, currentLeaderboard] = await Promise.all([
        getCurrentChallenge(),
        getCurrentChallengeStatus(),
        getCurrentLeaderboard()
      ]);

      let activeSession: GameSessionView | null = null;

      if (currentStatus.hasActiveSession) {
        activeSession = await getActiveCurrentSession();

        if (activeSession === null && currentStatus.activeSessionId !== undefined) {
          activeSession = await getSession(currentStatus.activeSessionId);
        }
      }

      setChallenge(currentChallenge);
      setStatus(currentStatus);
      setLeaderboard(currentLeaderboard);
      setSession(activeSession);
      setLoadState("ready");
    } catch (error) {
      setPageError(errorMessage(error));
      setLoadState("error");
    } finally {
      setLeaderboardLoading(false);
    }
  };

  useEffect(() => {
    void loadInitialState();
  }, []);

  const startSession = async () => {
    setSessionBusy(true);
    setActionError(null);
    setFeedback(null);

    try {
      const nextSession = await createOrReuseCurrentSession();
      setSession(nextSession);
      await Promise.all([refreshStatus(), refreshLeaderboard()]);
    } catch (error) {
      setActionError(errorMessage(error));
    } finally {
      setSessionBusy(false);
    }
  };

  const resumeSession = async () => {
    if (status === null) {
      return;
    }

    setSessionBusy(true);
    setActionError(null);
    setFeedback(null);

    try {
      const activeSession = await getActiveCurrentSession();

      if (activeSession !== null) {
        setSession(activeSession);
      } else if (status.activeSessionId !== undefined) {
        setSession(await getSession(status.activeSessionId));
      } else {
        throw new Error("No hay sesion activa para reanudar.");
      }

      await refreshStatus();
    } catch (error) {
      setActionError(errorMessage(error));
    } finally {
      setSessionBusy(false);
    }
  };

  const applyAction = async (action: "reveal" | "toggle-flag", row: number, col: number) => {
    if (session === null || session.status !== "IN_PROGRESS" || actionPending) {
      return;
    }

    setActionPending(true);
    setActionError(null);
    setFeedback(null);

    try {
      const response =
        action === "reveal"
          ? await revealCell(session.sessionId, row, col)
          : await toggleFlag(session.sessionId, row, col);

      setSession(response.session);
      setFeedback(actionResultText(response.action.result));

      if (response.session.status !== "IN_PROGRESS") {
        await Promise.all([refreshStatus(), refreshLeaderboard()]);
      }
    } catch (error) {
      setActionError(errorMessage(error));
    } finally {
      setActionPending(false);
    }
  };

  const renderSessionCta = () => {
    if (status === null || session?.status === "IN_PROGRESS") {
      return null;
    }

    if (status.status === "IN_PROGRESS" || status.canResumeSession) {
      return (
        <button type="button" className="button primary" onClick={resumeSession} disabled={sessionBusy}>
          {sessionBusy ? "Reanudando..." : "Reanudar"}
        </button>
      );
    }

    if (status.status === "NOT_PLAYED" || status.status === "WON" || status.status === "LOST") {
      const label = status.status === "NOT_PLAYED" ? "Empezar" : "Jugar otra vez";

      return (
        <button type="button" className="button primary" onClick={startSession} disabled={sessionBusy}>
          {sessionBusy ? "Preparando..." : label}
        </button>
      );
    }

    return null;
  };

  if (loadState === "loading") {
    return (
      <main className="app-shell loading-shell">
        <section className="panel loading-panel">
          <p className="eyebrow">MineDaily</p>
          <h1>Cargando challenge...</h1>
          <p className="muted">Preparando el challenge vigente.</p>
        </section>
      </main>
    );
  }

  if (loadState === "error" || challenge === null || status === null) {
    return (
      <main className="app-shell loading-shell">
        <section className="panel loading-panel">
          <p className="eyebrow">MineDaily</p>
          <h1>No se pudo cargar</h1>
          <p className="inline-error">{pageError ?? "Fallo la carga inicial."}</p>
          <button type="button" className="button primary" onClick={() => void loadInitialState()}>
            Reintentar
          </button>
        </section>
      </main>
    );
  }

  return (
    <main className="app-shell">
      <section className="game-column">
        <SessionHud challenge={challenge} status={status} session={session} />

        {actionError !== null && <p className="inline-error">{actionError}</p>}
        {feedback !== null && <p className="inline-success">{feedback}</p>}

        {session !== null ? (
          <section className="panel board-panel">
            <div className="panel-heading">
              <div>
                <p className="eyebrow">Sesion</p>
                <h2>{session.status === "IN_PROGRESS" ? "En juego" : "Resultado"}</h2>
              </div>
              {session.status !== "IN_PROGRESS" && renderSessionCta()}
            </div>

            <Board
              board={session.board}
              disabled={actionPending || session.status !== "IN_PROGRESS"}
              onReveal={(row, col) => void applyAction("reveal", row, col)}
              onToggleFlag={(row, col) => void applyAction("toggle-flag", row, col)}
            />
          </section>
        ) : (
          <section className="panel start-panel">
            <p className="eyebrow">Estado del dia</p>
            <h2>{statusSummary(status)}</h2>
            <p className="muted">Tenes 3 vidas para completar el tablero del dia.</p>
            {renderSessionCta()}
          </section>
        )}
      </section>

      <LeaderboardPanel
        leaderboard={leaderboard}
        loading={leaderboardLoading}
        errorMessage={leaderboardError}
        onRefresh={() => void refreshLeaderboard()}
      />
    </main>
  );
}

export default App;
