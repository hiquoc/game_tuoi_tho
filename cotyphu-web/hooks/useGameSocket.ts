"use client";

import { useCallback, useEffect, useRef, useState } from "react";
import { WS_URL } from "../lib/config";
import { RoomSnapshot } from "../lib/types";

/**
 * WebSocket connection to the game server. Receives full-state snapshots
 * after every action; sends one of the 7 legal actions. Auto-reconnects.
 */
export function useGameSocket(roomId: string | null, playerId: string | null) {
  const [snapshot, setSnapshot] = useState<RoomSnapshot | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [connected, setConnected] = useState(false);
  const wsRef = useRef<WebSocket | null>(null);

  useEffect(() => {
    if (!roomId || !playerId) return;
    let closed = false;
    let retry = 0;
    let timer: ReturnType<typeof setTimeout>;

    const connect = () => {
      const ws = new WebSocket(`${WS_URL}/ws/arena?roomId=${roomId}&playerId=${playerId}`);
      wsRef.current = ws;
      ws.onopen = () => {
        setConnected(true);
        setError(null);
        retry = 0;
      };
      ws.onmessage = (ev) => {
        try {
          const msg = JSON.parse(ev.data);
          if (msg.kind === "snapshot") {
            setSnapshot(msg.data);
            setError(null);
          } else if (msg.kind === "error") {
            setError(msg.message);
          }
        } catch {
          /* ignore malformed frames */
        }
      };
      ws.onclose = () => {
        setConnected(false);
        if (!closed) {
          retry += 1;
          timer = setTimeout(connect, Math.min(1000 * retry, 5000));
        }
      };
      ws.onerror = () => ws.close();
    };

    connect();
    return () => {
      closed = true;
      clearTimeout(timer);
      wsRef.current?.close();
    };
  }, [roomId, playerId]);

  const send = useCallback((action: string, tileIndex?: number) => {
    wsRef.current?.send(JSON.stringify({ action, tileIndex: tileIndex ?? null }));
  }, []);

  return { snapshot, error, connected, send, clearError: () => setError(null) };
}
