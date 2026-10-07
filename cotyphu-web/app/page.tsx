"use client";

import { useState } from "react";
import { useRouter } from "next/navigation";
import { createRoom, joinRoom } from "../lib/api";

const NAME_KEY = "arena:playerName";

export default function LobbyPage() {
  const router = useRouter();
  const [name, setName] = useState(() => {
    if (typeof window === "undefined") return "";
    return localStorage.getItem(NAME_KEY) ?? "";
  });
  const [roomCode, setRoomCode] = useState("");
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState("");

  const goToRoom = (roomId: string, playerId: string) => {
    sessionStorage.setItem(`arena:${roomId}:playerId`, playerId);
    sessionStorage.setItem(`arena:${roomId}:playerName`, name.trim());
    localStorage.setItem(NAME_KEY, name.trim());
    router.push(`/room/${roomId}`);
  };

  const handleCreate = async () => {
    if (!name.trim()) return setError("Nhập tên của bạn trước nhé");
    setBusy(true);
    setError("");
    try {
      const { roomId, playerId } = await createRoom(name.trim());
      goToRoom(roomId, playerId);
    } catch (e) {
      setError("Không tạo được phòng. Server đã chạy chưa?");
    } finally {
      setBusy(false);
    }
  };

  const handleJoin = async () => {
    if (!name.trim()) return setError("Nhập tên của bạn trước nhé");
    if (!roomCode.trim()) return setError("Nhập mã phòng");
    setBusy(true);
    setError("");
    try {
      const { playerId } = await joinRoom(roomCode.trim(), name.trim());
      goToRoom(roomCode.trim(), playerId);
    } catch (e) {
      setError("Không vào được phòng. Kiểm tra lại mã phòng nhé.");
    } finally {
      setBusy(false);
    }
  };

  return (
    <div className="lobby-wrap">
      <div className="lobby-card pop-in">
        <img src="/logo.png" alt="Sân Chơi Tuổi Thơ" className="lobby-logo floaty" />
        <h1>🎲 Sân Chơi Tuổi Thơ</h1>
        <p className="sub">Cờ Tỷ Phú multiplayer — server quản lý toàn bộ trạng thái</p>

        <div className="field">
          <label>Tên của bạn</label>
          <input
            value={name}
            onChange={(e) => setName(e.target.value)}
            placeholder="VD: An"
            maxLength={20}
          />
        </div>

        <button className="btn" style={{ width: "100%" }} onClick={handleCreate} disabled={busy}>
          {busy ? "Đang tạo..." : "Tạo phòng mới"}
        </button>

        <div className="divider">hoặc</div>

        <div className="field">
          <label>Mã phòng</label>
          <input
            value={roomCode}
            onChange={(e) => setRoomCode(e.target.value)}
            placeholder="VD: a1b2c3d4"
            maxLength={8}
          />
        </div>
        <button className="btn secondary" style={{ width: "100%" }} onClick={handleJoin} disabled={busy}>
          Vào phòng
        </button>

        <div className="form-error">{error}</div>
      </div>
    </div>
  );
}
