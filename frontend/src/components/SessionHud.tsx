import type {
  CurrentChallengeView,
  CurrentUserChallengeStatusView,
  GameSessionView
} from "../types/minedaily";
import { formatDateTime, formatDurationMs } from "../utils/format";

interface SessionHudProps {
  challenge: CurrentChallengeView;
  status: CurrentUserChallengeStatusView;
  session: GameSessionView | null;
}

function statusText(status: string): string {
  switch (status) {
    case "NOT_PLAYED":
      return "Sin jugar";
    case "IN_PROGRESS":
      return "En curso";
    case "WON":
      return "Ganada";
    case "LOST":
      return "Perdida";
    default:
      return status;
  }
}

export function SessionHud({ challenge, status, session }: SessionHudProps) {
  const remainingLives = session?.lives.remainingLives ?? status.remainingLives;
  const maxLives = session?.lives.maxLives ?? status.maxLives;
  const sessionStatus = session?.status ?? status.status;

  return (
    <section className="panel hud-panel" aria-label="Estado de la partida">
      <div>
        <p className="eyebrow">Challenge vigente</p>
        <h1>MineDaily</h1>
        <p className="muted">
          {challenge.challengeDate} - {challenge.board.rows}x{challenge.board.cols} -{" "}
          {challenge.board.mineCount} minas
        </p>
      </div>

      <div className="hud-grid">
        <div className="metric">
          <span>Estado</span>
          <strong>{statusText(sessionStatus)}</strong>
        </div>
        <div className="metric">
          <span>Vidas</span>
          <strong>
            {remainingLives ?? "-"} / {maxLives ?? "-"}
          </strong>
        </div>
        <div className="metric">
          <span>Errores</span>
          <strong>{session?.performance.errorCount ?? "-"}</strong>
        </div>
        <div className="metric">
          <span>Clicks</span>
          <strong>{session?.performance.clickCount ?? "-"}</strong>
        </div>
        <div className="metric">
          <span>Tiempo</span>
          <strong>{formatDurationMs(session?.durationMs)}</strong>
        </div>
        <div className="metric">
          <span>Rollover</span>
          <strong>{formatDateTime(challenge.rolloverAt)}</strong>
        </div>
      </div>
    </section>
  );
}
