import { PlayerView, TileView, TyPhuSnapshot } from "./types";

export const KIND_LABEL: Record<string, string> = {
  GO: "Xuất phát",
  PROPERTY: "Đất",
  STATION: "Nhà ga",
  UTILITY: "Tiện ích",
  TAX: "Thuế",
  CHANCE: "Cơ hội",
  COMMUNITY: "Khí vận",
  JAIL: "Nhà tù",
  GO_TO_JAIL: "Đi tù",
  FREE_PARKING: "Bãi đỗ xe",
};

/**
 * Display-only rent estimate. The server is the source of truth;
 * this just previews what the rent would roughly be.
 */
export function rentEstimate(snap: TyPhuSnapshot, tile: TileView): string | null {
  if (tile.kind === "PROPERTY" && tile.price != null) {
    const base = Math.max(1, Math.floor(tile.price / 30));
    if (!tile.ownerId) return `~${base} (x2 nếu trọn bộ màu)`;
    const owner = snap.players.find((p) => p.id === tile.ownerId);
    if (!owner) return null;
    const houses = owner.owned.find((o) => o.tileIndex === tile.index)?.houses ?? 0;
    if (houses > 0) {
      const mult = [1, 5, 15, 45, 80, 125];
      return `${base * mult[houses]} (${houses >= 5 ? "khách sạn" : `${houses} nhà`})`;
    }
    const full = snap.board
      .filter((t) => t.group === tile.group)
      .every((t) => t.ownerId === owner.id);
    return `${full ? base * 2 : base}${full ? " (trọn bộ màu)" : ""}`;
  }
  if (tile.kind === "STATION") {
    if (!tile.ownerId) return "25 / 50 / 100 / 200 theo số ga sở hữu";
    const n = snap.board.filter(
      (t) => t.kind === "STATION" && t.ownerId === tile.ownerId
    ).length;
    return `${25 * Math.pow(2, n - 1)} (đang có ${n} ga)`;
  }
  if (tile.kind === "UTILITY") {
    if (!tile.ownerId) return "4x hoặc 10x tổng xúc xắc";
    const n = snap.board.filter(
      (t) => t.kind === "UTILITY" && t.ownerId === tile.ownerId
    ).length;
    return `${n === 2 ? "10x" : "4x"} tổng xúc xắc`;
  }
  if (tile.kind === "TAX") return tile.index === 4 ? "Nộp 200" : "Nộp 100";
  return null;
}

/** Display-only net worth: cash + properties + buildings. */
export function netWorth(snap: TyPhuSnapshot, p: PlayerView): number {
  let w = p.money;
  for (const o of p.owned) {
    const t = snap.board[o.tileIndex];
    w += t?.price ?? 0;
    w += o.houses * (t?.houseCost ?? 0);
  }
  return w;
}
