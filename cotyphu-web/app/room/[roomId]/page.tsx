"use client";

import { useEffect, useState } from "react";
import { useParams, useRouter } from "next/navigation";
import { useGameSocket } from "../../../hooks/useGameSocket";
import { getRoom, startGame } from "../../../lib/api";
import { isLobby, PLAYER_COLORS, RoomSnapshot } from "../../../lib/types";
import Board, { BoardCenter } from "../../../components/Board";
import { ActionBar, EventLog, PlayerList } from "../../../components/SidePanel";

export default function RoomPage() {
  const params = useParams();
  const router = useRouter();
  const roomId = params.roomId as string;
  const [playerId, setPlayerId] = useState<string | null>(null);
  const [starting, setStarting] = useState(false);

  useEffect(() => {
    const pid = sessionStorage.getItem(`arena:${roomId}:playerId`);
    if (!pid) router.replace("/");
    else setPlayerId(pid);
  }, [roomId, router]);

  const { snapshot, error, connected, send, clearError } = useGameSocket(roomId, playerId);

  // Fallback: poll the lobby until the game starts (covers any missed WS broadcast).
  useEffect(() => {
    if (!playerId || (snapshot && !isLobby(snapshot))) return;
    let alive = true;
    const poll = async () => {
      try {
        const data: RoomSnapshot = await getRoom(roomId);
        if (alive && (!snapshot || isLobby(snapshot))) {
          // Only adopt REST state while still in lobby; WS owns the game.
          window.dispatchEvent(new CustomEvent("arena:lobby", { detail: data }));
        }
      } catch {
        /* server may not be up yet */
      }
    };
    const t = setInterval(poll, 2500);
    poll();
    return () => {
      alive = false;
      clearInterval(t);
    };
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [roomId, playerId, snapshot === null || isLobby(snapshot)]);

  // Adopt lobby snapshots delivered via the polling fallback.
  const [lobbyFallback, setLobbyFallback] = useState<RoomSnapshot | null>(null);
  useEffect(() => {
    const handler = (e: Event) =>
      setLobbyFallback((e as CustomEvent<RoomSnapshot>).detail);
    window.addEventListener("arena:lobby", handler);
    return () => window.removeEventListener("arena:lobby", handler);
  }, []);

  const effective: RoomSnapshot | null = snapshot ?? lobbyFallback;

  if (!playerId) return null;

  if (!effective) {
    return (
      <div className="lobby-wrap">
        <div className="lobby-card">
          <p>Đang kết nối tới phòng...</p>
          <div className="conn off">Kiểm tra server backend đã chạy ở cổng 8080</div>
        </div>
      </div>
    );
  }

  if (isLobby(effective)) {
    const names = Object.entries(effective.players);
    return (
      <div className="lobby-wrap">
        <div className="wait-card">
          <h2>Phòng chờ</h2>
          <div className="room-code">{effective.roomId}</div>
          <p className="sub" style={{ opacity: 0.7, fontSize: 13 }}>
            Gửi mã này cho bạn bè để vào phòng (2–6 người)
          </p>
          <ul className="wait-list">
            {names.map(([id, name], i) => (
              <li key={id}>
                <div className="dot" style={{ background: PLAYER_COLORS[i % PLAYER_COLORS.length] }} />
                {name}
                {id === playerId && " (bạn)"}
              </li>
            ))}
          </ul>
          <button
            className="btn"
            disabled={starting || names.length < 2}
            onClick={async () => {
              setStarting(true);
              try {
                await startGame(effective.roomId);
              } catch {
                /* WS broadcast / polling will surface the state */
              } finally {
                setStarting(false);
              }
            }}
          >
            {starting ? "Đang bắt đầu..." : `Bắt đầu ván (${names.length}/6)`}
          </button>
          {names.length < 2 && (
            <div className="form-error">Cần ít nhất 2 người để bắt đầu</div>
          )}
          <div className="conn" style={{ marginTop: 12 }}>
            {connected ? "Đã kết nối" : "Đang kết nối lại..."}
          </div>
        </div>
      </div>
    );
  }

  const winner = effective.winnerId
    ? effective.players.find((p) => p.id === effective.winnerId)
    : null;

  return (
    <div className="game-layout">
      <div className="board-wrap">
        <Board snapshot={effective} center={<BoardCenter snapshot={effective} myId={playerId} />} />
      </div>
      <div className="side">
        {error && (
          <div className="error-toast">
            <span>{error}</span>
            <button onClick={clearError}>✕</button>
          </div>
        )}
        {effective.phase === "FINISHED" && winner && (
          <div className="card winner-banner">🏆 {winner.name} thắng!</div>
        )}
        <PlayerList snapshot={effective} myId={playerId} />
        <ActionBar snapshot={effective} myId={playerId} send={send} />
        <EventLog snapshot={effective} />
        <div className={`conn${connected ? "" : " off"}`}>
          {connected ? "● Đã kết nối" : "○ Đang kết nối lại..."}
        </div>
      </div>
    </div>
  );
}
