/** Mirrors the backend TyPhuSnapshot JSON. The client never computes game rules. */

export interface OwnedView {
  tileIndex: number;
  tileName: string;
  houses: number;
}

export interface PlayerView {
  id: string;
  name: string;
  money: number;
  position: number;
  inJail: boolean;
  bankrupt: boolean;
  jailFreeCards: number;
  owned: OwnedView[];
}

export interface TileView {
  index: number;
  name: string;
  kind: string;
  group: string | null;
  price: number | null;
  houseCost: number | null;
  ownerId: string | null;
}

export interface TyPhuSnapshot {
  phase: string;
  players: PlayerView[];
  currentPlayerId: string;
  dice1: number;
  dice2: number;
  hasRolled: boolean;
  pendingBuy: number | null;
  winnerId: string | null;
  log: string[];
  board: TileView[];
}

export interface LobbySnapshot {
  lobby: true;
  roomId: string;
  gameCode: string;
  players: Record<string, string>;
}

export type RoomSnapshot = TyPhuSnapshot | LobbySnapshot;

export function isLobby(s: RoomSnapshot | null): s is LobbySnapshot {
  return !!s && (s as LobbySnapshot).lobby === true;
}

/** Fixed player colors by seat order. */
export const PLAYER_COLORS = ["#e74c3c", "#3498db", "#2ecc71", "#f39c12", "#9b59b6", "#1abc9c"];

/** Property group -> board color. */
export const GROUP_COLORS: Record<string, string> = {
  "Nâu": "#7b4b2a",
  "Xanh nhạt": "#a8d8f0",
  "Hồng": "#e77fb0",
  "Cam": "#f5a623",
  "Đỏ": "#d0021b",
  "Vàng": "#f8e71c",
  "Xanh lá": "#2e9e5b",
  "Xanh dương": "#2c3ed6",
};

export const DICE_FACES = ["", "⚀", "⚁", "⚂", "⚃", "⚄", "⚅"];
