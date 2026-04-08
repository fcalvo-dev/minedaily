import type { MouseEvent } from "react";
import type { BoardSnapshotView, CellView } from "../types/minedaily";

interface BoardProps {
  board: BoardSnapshotView;
  disabled: boolean;
  onReveal: (row: number, col: number) => void;
  onToggleFlag: (row: number, col: number) => void;
}

function cellContent(cell: CellView): string {
  if (cell.state === "FLAGGED") {
    return "F";
  }

  if (cell.state === "REVEALED_MINE") {
    return "*";
  }

  if (cell.state === "REVEALED_SAFE" && cell.adjacentMineCount !== null && cell.adjacentMineCount > 0) {
    return String(cell.adjacentMineCount);
  }

  return "";
}

function cellLabel(cell: CellView): string {
  if (cell.state === "FLAGGED") {
    return `Bandera en fila ${cell.row + 1}, columna ${cell.col + 1}`;
  }

  if (cell.state === "REVEALED_MINE") {
    return `Mina revelada en fila ${cell.row + 1}, columna ${cell.col + 1}`;
  }

  if (cell.state === "REVEALED_SAFE") {
    return `Celda revelada en fila ${cell.row + 1}, columna ${cell.col + 1}`;
  }

  return `Celda oculta en fila ${cell.row + 1}, columna ${cell.col + 1}`;
}

function isRevealed(cell: CellView): boolean {
  return cell.state === "REVEALED_SAFE" || cell.state === "REVEALED_MINE";
}

function cellClassName(cell: CellView): string {
  return `board-cell board-cell-${cell.state.toLowerCase().replace(/_/g, "-")}`;
}

export function Board({ board, disabled, onReveal, onToggleFlag }: BoardProps) {
  const handleReveal = (cell: CellView) => {
    if (!disabled && cell.state === "HIDDEN") {
      onReveal(cell.row, cell.col);
    }
  };

  const handleContextMenu = (event: MouseEvent<HTMLButtonElement>, cell: CellView) => {
    event.preventDefault();

    if (!disabled && (cell.state === "HIDDEN" || cell.state === "FLAGGED")) {
      onToggleFlag(cell.row, cell.col);
    }
  };

  return (
    <div className="board-wrap" aria-label="Tablero visible">
      <div
        className="board-grid"
        style={{ gridTemplateColumns: `repeat(${board.cols}, minmax(30px, 1fr))` }}
      >
        {board.cells.flat().map((cell) => (
          <button
            key={`${cell.row}-${cell.col}`}
            type="button"
            className={cellClassName(cell)}
            disabled={disabled || isRevealed(cell)}
            aria-label={cellLabel(cell)}
            onClick={() => handleReveal(cell)}
            onContextMenu={(event) => handleContextMenu(event, cell)}
          >
            {cellContent(cell)}
          </button>
        ))}
      </div>
      <p className="board-help">Click izquierdo revela. Click derecho marca o quita bandera.</p>
    </div>
  );
}
