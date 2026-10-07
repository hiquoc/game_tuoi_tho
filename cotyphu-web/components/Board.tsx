"use client";

import { GROUP_COLORS, PLAYER_COLORS, PlayerView, TileView, TyPhuSnapshot, DICE_FACES } from "../lib/types";

/** Grid position of tile i on the 11x11 board. */
export function tileGridPos(i: number): { row: number; col: number } {
  if (i === 0) return { row: 11, col: 11 };
  if (i < 10) return { row: 11, col: 11 - i };
  if (i === 10) return { row: 11, col: 1 };
  if (i < 20) return { row: 11 - (i - 10), col: 1 };
  if (i === 20) return { row: 1, col: 1 };
  if (i < 30) return { row: 1, col: 1 + (i - 20) };
  if (i === 30) return { row: 1, col: 11 };
  return { row: 1 + (i - 30), col: 11 };
}

const CORNER_ICON: Record<string, string> = {
  GO: "🚩",
  JAIL: "🔒",
  FREE_PARKING: "🅿️",
  GO_TO_JAIL: "🚔",
};

function ownerColor(ownerId: string | null, players: PlayerView[]): string | null {
  if (!ownerId) return null;
  const idx = players.findIndex((p) => p.id === ownerId);
  return idx >= 0 ? PLAYER_COLORS[idx % PLAYER_COLORS.length] : "#888";
}

function TileCell({ tile, players }: { tile: TileView; players: PlayerView[] }) {
  const { row, col } = tileGridPos(tile.index);
  const isCorner = ["GO", "JAIL", "FREE_PARKING", "GO_TO_JAIL"].includes(tile.kind);
  const oColor = ownerColor(tile.ownerId, players);
  const onTile = players.filter((p) => p.position === tile.index && !p.bankrupt);
  const houses = players
    .flatMap((p) => p.owned)
    .find((o) => o.tileIndex === tile.index)?.houses;

  return (
    <div
      className={`tile${isCorner ? " corner" : ""}`}
      style={{ gridRow: row, gridColumn: col }}
      title={tile.name}
    >
      {tile.group && GROUP_COLORS[tile.group] && (
        <div className="colorbar" style={{ background: GROUP_COLORS[tile.group] }} />
      )}
      {isCorner ? (
        <>
          <div className="big">{CORNER_ICON[tile.kind] ?? "⬜"}</div>
          <div>{tile.name}</div>
        </>
      ) : (
        <>
          <div className="tname">{tile.name}</div>
          {tile.price != null && <div className="tprice">{tile.price}</div>}
        </>
      )}
      {houses != null && houses > 0 && (
        <div className="houses">{houses >= 5 ? "🏨" : "🏠".repeat(houses)}</div>
      )}
      {onTile.length > 0 && (
        <div className="tokens">
          {onTile.map((p) => {
            const idx = players.findIndex((x) => x.id === p.id);
            return (
              <div
                key={p.id}
                className="token"
                style={{ background: PLAYER_COLORS[idx % PLAYER_COLORS.length] }}
                title={p.name}
              >
                {p.name.charAt(0).toUpperCase()}
              </div>
            );
          })}
        </div>
      )}
      {oColor && (
        <div className="ownerbar" style={{ background: oColor }} />
      )}
    </div>
  );
}

export default function Board({
  snapshot,
  center,
}: {
  snapshot: TyPhuSnapshot;
  center: React.ReactNode;
}) {
  return (
    <div className="board">
      {snapshot.board.map((t) => (
        <TileCell key={t.index} tile={t} players={snapshot.players} />
      ))}
      <div className="center-panel">{center}</div>
    </div>
  );
}

/** Dice + turn indicator shown in the middle of the board. */
export function BoardCenter({ snapshot, myId }: { snapshot: TyPhuSnapshot; myId: string }) {
  const current = snapshot.players.find((p) => p.id === snapshot.currentPlayerId);
  const isMe = snapshot.currentPlayerId === myId;
  const rolled = snapshot.dice1 > 0;

  return (
    <>
      <h2>Cờ Tỷ Phú</h2>
      <div className="dice">
        {rolled ? `${DICE_FACES[snapshot.dice1]}${DICE_FACES[snapshot.dice2]}` : "🎲🎲"}
      </div>
      <div className="turn">
        {snapshot.phase === "FINISHED"
          ? "Ván đã kết thúc"
          : isMe
            ? "Tới lượt bạn!"
            : `Lượt của ${current?.name ?? "..."}`}
      </div>
    </>
  );
}
