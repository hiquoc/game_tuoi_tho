"use client";

import { useEffect, useRef, useState } from "react";
import { TyPhuSnapshot } from "../lib/types";

export interface CardDraw {
  key: string;
  playerName: string;
  playerIndex: number;
  text: string;
  deck: "chance" | "community";
}

/**
 * Watches the event log for new card draws ("<name> rút thẻ: <text>").
 * The snapshot carries no explicit draw event, so the log line is the
 * signal — every snapshot is a fresh server broadcast, so scanning only
 * the entries that arrived since the last snapshot is reliable.
 */
export function useCardDraw(snapshot: TyPhuSnapshot | null): {
  draw: CardDraw | null;
  dismiss: () => void;
} {
  const [draw, setDraw] = useState<CardDraw | null>(null);
  const prevLen = useRef(0);

  useEffect(() => {
    if (!snapshot) return;
    const log = snapshot.log;
    // Entries that arrived with this snapshot (log is capped at 60 server-side).
    const fresh = log.length >= prevLen.current ? log.slice(prevLen.current) : log.slice(-5);
    prevLen.current = log.length;

    const lines = fresh.filter((l) => /rút thẻ:/.test(l));
    if (lines.length === 0) return;
    const line = lines[lines.length - 1];
    const m = line.match(/^(.*) rút thẻ: (.*)$/);
    if (!m) return;

    const playerIndex = snapshot.players.findIndex((p) => p.name === m[1]);
    const player = playerIndex >= 0 ? snapshot.players[playerIndex] : null;
    const tile = player ? snapshot.board[player.position] : undefined;
    const deck = tile?.kind === "COMMUNITY" ? "community" : "chance";

    setDraw({
      key: `${log.length}::${line}`,
      playerName: m[1],
      playerIndex: Math.max(0, playerIndex),
      text: m[2],
      deck,
    });
  }, [snapshot]);

  return { draw, dismiss: () => setDraw(null) };
}
