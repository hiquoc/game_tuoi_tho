"use client";

import { useState } from "react";
import Avatar from "./Avatar";

/** Waiting room. Player list updates in real time over the WebSocket —
 *  the server broadcasts a fresh lobby snapshot on every join/leave. */
export default function LobbyView({
  roomId,
  players,
  playerId,
  connected,
  starting,
  onStart,
  onLeave,
}: {
  roomId: string;
  players: Record<string, string>;
  playerId: string;
  connected: boolean;
  starting: boolean;
  onStart: () => void;
  onLeave: () => void;
}) {
  const entries = Object.entries(players);
  const [copied, setCopied] = useState(false);

  const copyCode = async () => {
    try {
      await navigator.clipboard.writeText(roomId);
      setCopied(true);
      setTimeout(() => setCopied(false), 1500);
    } catch {
      /* clipboard unavailable */
    }
  };

  return (
    <div className="lobby-wrap">
      <div className="wait-card pop-in">
        <img src="/logo.png" alt="Sân Chơi Tuổi Thơ" className="lobby-logo floaty" />
        <h2>Cờ Tỷ Phú — Phòng chờ</h2>
        <div className="room-code-row">
          <div className="room-code">{roomId}</div>
          <button className="btn small secondary" onClick={copyCode}>
            {copied ? "Đã chép ✓" : "Chép mã"}
          </button>
        </div>
        <p className="hint">Gửi mã này cho bạn bè để vào phòng (2–6 người)</p>
        <ul className="wait-list">
          {entries.map(([id, name], i) => (
            <li key={id} className="slide-in">
              <Avatar index={i} name={name} size={36} />
              <span style={{ flex: 1 }}>{name}</span>
              {i === 0 && <span className="host-badge">Chủ phòng</span>}
              {id === playerId && <span className="me-badge">bạn</span>}
            </li>
          ))}
        </ul>
        <div className="btn-row" style={{ justifyContent: "center" }}>
          <button
            className="btn"
            disabled={starting || entries.length < 2}
            onClick={onStart}
          >
            {starting ? "Đang bắt đầu..." : `Bắt đầu ván (${entries.length}/6)`}
          </button>
          <button className="btn secondary" onClick={onLeave}>
            Rời phòng
          </button>
        </div>
        {entries.length < 2 && (
          <div className="form-error">Cần ít nhất 2 người để bắt đầu</div>
        )}
        <div className="conn" style={{ marginTop: 12 }}>
          {connected ? "● Đang nghe phòng trực tiếp" : "○ Đang kết nối lại..."}
        </div>
      </div>
    </div>
  );
}
