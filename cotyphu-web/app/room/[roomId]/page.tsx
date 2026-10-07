"use client";

import { useCallback, useEffect, useRef, useState } from "react";
import { useParams, useRouter } from "next/navigation";
import { useGameSocket } from "../../../hooks/useGameSocket";
import { useCardDraw } from "../../../hooks/useCardDraw";
import { joinRoom, leaveRoom, startGame } from "../../../lib/api";
import { isLobby } from "../../../lib/types";
import Board, { BoardCenter } from "../../../components/Board";
import { ActionBar, EventLog, PlayerList, TileDetail } from "../../../components/SidePanel";
import LobbyView from "../../../components/LobbyView";
import CardDrawModal from "../../../components/CardDrawModal";

const pidKey = (roomId: string) => `arena:${roomId}:playerId`;
const nameKey = (roomId: string) => `arena:${roomId}:playerName`;

export default function RoomPage() {
  const params = useParams();
  const router = useRouter();
  const roomId = params.roomId as string;
  const [playerId, setPlayerId] = useState<string | null>(null);
  const [starting, setStarting] = useState(false);
  const [selectedTile, setSelectedTile] = useState<number | null>(null);
  const rejoining = useRef(false);
  const leaving = useRef(false);

  useEffect(() => {
    const pid = sessionStorage.getItem(pidKey(roomId));
    if (!pid) router.replace("/");
    else setPlayerId(pid);
  }, [roomId, router]);

  const { snapshot, error, connected, send, clearError } = useGameSocket(roomId, playerId);
  const gameSnapshot = snapshot && !isLobby(snapshot) ? snapshot : null;
  const { draw, dismiss } = useCardDraw(gameSnapshot);

  // Keyboard shortcuts: R roll, B buy, E end turn (only on your turn).
  useEffect(() => {
    if (!gameSnapshot || !playerId) return;
    const handler = (e: KeyboardEvent) => {
      const tag = (e.target as HTMLElement)?.tagName;
      if (tag === "INPUT" || tag === "TEXTAREA") return;
      const me = gameSnapshot.players.find((p) => p.id === playerId);
      if (!me || me.bankrupt || gameSnapshot.currentPlayerId !== playerId) return;
      const k = e.key.toLowerCase();
      if (k === "r" && !gameSnapshot.hasRolled) send("ROLL_DICE");
      else if (k === "b" && gameSnapshot.pendingBuy != null) send("BUY_PROPERTY");
      else if (k === "e" && gameSnapshot.hasRolled && gameSnapshot.pendingBuy == null)
        send("END_TURN");
    };
    window.addEventListener("keydown", handler);
    return () => window.removeEventListener("keydown", handler);
  }, [gameSnapshot, playerId, send]);

  // Auto re-join: the server drops disconnected clients from the lobby, so a
  // refresh can leave us with a playerId the lobby no longer knows. In that
  // case join again with the stored name and keep playing.
  useEffect(() => {
    if (!snapshot || !isLobby(snapshot) || !playerId || rejoining.current || leaving.current)
      return;
    if (snapshot.players[playerId]) return;
    const name = sessionStorage.getItem(nameKey(roomId));
    if (!name) {
      router.replace("/");
      return;
    }
    rejoining.current = true;
    joinRoom(roomId, name)
      .then(({ playerId: nid }) => {
        sessionStorage.setItem(pidKey(roomId), nid);
        setPlayerId(nid);
      })
      .catch(() => router.replace("/"))
      .finally(() => {
        rejoining.current = false;
      });
  }, [snapshot, playerId, roomId, router]);

  const handleLeave = useCallback(async () => {
    leaving.current = true;
    if (playerId) {
      try {
        await leaveRoom(roomId, playerId);
      } catch {
        /* already gone */
      }
      sessionStorage.removeItem(pidKey(roomId));
      sessionStorage.removeItem(nameKey(roomId));
    }
    router.push("/");
  }, [roomId, playerId, router]);

  const handleStart = useCallback(async () => {
    setStarting(true);
    try {
      await startGame(roomId);
    } catch {
      /* the WS broadcast carries the new state */
    } finally {
      setStarting(false);
    }
  }, [roomId]);

  if (!playerId) return null;

  if (!snapshot) {
    return (
      <div className="lobby-wrap">
        <div className="lobby-card pop-in">
          <p>Đang kết nối tới phòng...</p>
          <div className="conn off">Kiểm tra server backend đã chạy ở cổng 8080</div>
        </div>
      </div>
    );
  }

  if (isLobby(snapshot)) {
    return (
      <LobbyView
        roomId={roomId}
        players={snapshot.players}
        playerId={playerId}
        connected={connected}
        starting={starting}
        onStart={handleStart}
        onLeave={handleLeave}
      />
    );
  }

  const winner = snapshot.winnerId
    ? snapshot.players.find((p) => p.id === snapshot.winnerId)
    : null;

  return (
    <div className="game-layout">
      <CardDrawModal draw={draw} onClose={dismiss} />
      <div className="board-wrap">
        <Board
          snapshot={snapshot}
          selectedTile={selectedTile}
          onSelectTile={setSelectedTile}
          center={<BoardCenter snapshot={snapshot} myId={playerId} />}
        />
      </div>
      <div className="side">
        {error && (
          <div className="error-toast">
            <span>{error}</span>
            <button onClick={clearError}>✕</button>
          </div>
        )}
        {snapshot.phase === "FINISHED" && winner && (
          <div className="card winner-banner pop-in">🏆 {winner.name} thắng!</div>
        )}
        <TileDetail
          snapshot={snapshot}
          tileIndex={selectedTile}
          onClose={() => setSelectedTile(null)}
        />
        <PlayerList snapshot={snapshot} myId={playerId} />
        <ActionBar snapshot={snapshot} myId={playerId} send={send} />
        <EventLog snapshot={snapshot} />
        <div className={`conn${connected ? "" : " off"}`}>
          {connected ? "● Đã kết nối" : "○ Đang kết nối lại..."}
        </div>
      </div>
    </div>
  );
}
