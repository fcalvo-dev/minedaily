import type { DailyLeaderboardView } from "../types/minedaily";
import { formatDurationMs } from "../utils/format";

interface LeaderboardPanelProps {
  leaderboard: DailyLeaderboardView | null;
  loading: boolean;
  errorMessage: string | null;
  onRefresh: () => void;
}

export function LeaderboardPanel({
  leaderboard,
  loading,
  errorMessage,
  onRefresh
}: LeaderboardPanelProps) {
  return (
    <aside className="panel leaderboard-panel" aria-label="Leaderboard del challenge">
      <div className="panel-heading">
        <div>
          <p className="eyebrow">Leaderboard</p>
          <h2>Top del dia</h2>
        </div>
        <button type="button" className="button secondary" onClick={onRefresh} disabled={loading}>
          Actualizar
        </button>
      </div>

      {errorMessage !== null && <p className="inline-error">{errorMessage}</p>}

      {loading && leaderboard === null ? (
        <p className="muted">Cargando ranking...</p>
      ) : leaderboard === null || leaderboard.entries.length === 0 ? (
        <p className="muted">Todavia no hay victorias elegibles.</p>
      ) : (
        <table className="leaderboard-table">
          <thead>
            <tr>
              <th>#</th>
              <th>Jugador</th>
              <th>Tiempo</th>
              <th>Errores</th>
              <th>Clicks</th>
            </tr>
          </thead>
          <tbody>
            {leaderboard.entries.map((entry) => (
              <tr key={`${entry.position}-${entry.displayName}-${entry.endedAt}`}>
                <td>{entry.position}</td>
                <td>{entry.displayName}</td>
                <td>{formatDurationMs(entry.durationMs)}</td>
                <td>{entry.errorCount}</td>
                <td>{entry.clickCount}</td>
              </tr>
            ))}
          </tbody>
        </table>
      )}
    </aside>
  );
}
