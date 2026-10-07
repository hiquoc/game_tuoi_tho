"use client";

import { GROUP_COLORS, PlayerView, TyPhuSnapshot } from "../lib/types";
import { KIND_LABEL, netWorth, rentEstimate } from "../lib/gameInfo";
import Avatar from "./Avatar";

function ownsFullSet(snapshot: TyPhuSnapshot, player: PlayerView, tileIndex: number): boolean {
  const tile = snapshot.board[tileIndex];
  if (!tile || tile.kind !== "PROPERTY" || !tile.group) return false;
  return snapshot.board
    .filter((t) => t.group === tile.group)
    .every((t) => t.ownerId === player.id);
}

export function PlayerList({ snapshot, myId }: { snapshot: TyPhuSnapshot; myId: string }) {
  return (
    <div className="card">
      <h3>Người chơi</h3>
      {snapshot.players.map((p, i) => (
        <div
          key={p.id}
          className={`player-row${p.id === snapshot.currentPlayerId && snapshot.phase === "IN_PROGRESS" ? " active" : ""}`}
        >
          <Avatar index={i} name={p.name} size={36} />
          <div className="pinfo">
            <div className="pname">
              {p.name}
              {p.id === myId && " (bạn)"}
            </div>
            <div className="psub">
              {p.owned.length} đất · tài sản ~{netWorth(snapshot, p)}
            </div>
          </div>
          {p.inJail && <span className="badge jail">Tù</span>}
          {p.bankrupt && <span className="badge">Phá sản</span>}
          <div className="pmoney">{p.money}</div>
        </div>
      ))}
    </div>
  );
}

export function TileDetail({
  snapshot,
  tileIndex,
  onClose,
}: {
  snapshot: TyPhuSnapshot;
  tileIndex: number | null;
  onClose: () => void;
}) {
  if (tileIndex == null) return null;
  const tile = snapshot.board[tileIndex];
  if (!tile) return null;
  const owner = tile.ownerId ? snapshot.players.find((p) => p.id === tile.ownerId) : null;
  const houses = owner?.owned.find((o) => o.tileIndex === tile.index)?.houses ?? 0;
  const rent = rentEstimate(snapshot, tile);

  return (
    <div className="card tile-detail pop-in">
      {tile.group && GROUP_COLORS[tile.group] && (
        <div className="td-colorbar" style={{ background: GROUP_COLORS[tile.group] }} />
      )}
      <div className="td-head">
        <h3 style={{ margin: 0 }}>{tile.name}</h3>
        <button className="td-close" onClick={onClose} aria-label="Đóng">
          ✕
        </button>
      </div>
      <div className="td-rows">
        <div>
          <span>Loại ô</span>
          <b>{KIND_LABEL[tile.kind] ?? tile.kind}</b>
        </div>
        {tile.price != null && (
          <div>
            <span>Giá mua</span>
            <b>{tile.price}</b>
          </div>
        )}
        {tile.houseCost != null && (
          <div>
            <span>Giá xây nhà</span>
            <b>{tile.houseCost}</b>
          </div>
        )}
        <div>
          <span>Chủ sở hữu</span>
          <b>{owner ? owner.name : "Ngân hàng"}</b>
        </div>
        {houses > 0 && (
          <div>
            <span>Công trình</span>
            <b>{houses >= 5 ? "🏨 Khách sạn" : `🏠 ${houses} nhà`}</b>
          </div>
        )}
        {rent && (
          <div>
            <span>Tiền thuê</span>
            <b>{rent}</b>
          </div>
        )}
      </div>
    </div>
  );
}

export function ActionBar({
  snapshot,
  myId,
  send,
}: {
  snapshot: TyPhuSnapshot;
  myId: string;
  send: (action: string, tileIndex?: number) => void;
}) {
  const me = snapshot.players.find((p) => p.id === myId);
  if (!me || snapshot.phase !== "IN_PROGRESS" || me.bankrupt) return null;
  const myTurn = snapshot.currentPlayerId === myId;

  const buildable = me.owned.filter(
    (o) =>
      o.houses < 5 &&
      ownsFullSet(snapshot, me, o.tileIndex) &&
      (snapshot.board[o.tileIndex]?.houseCost ?? Infinity) <= me.money
  );

  const pendingTile =
    snapshot.pendingBuy != null ? snapshot.board[snapshot.pendingBuy] : null;

  return (
    <div className="card actions">
      <h3>Hành động</h3>
      {!myTurn && <div style={{ fontSize: 13, opacity: 0.7 }}>Chờ tới lượt bạn...</div>}
      {myTurn && (
        <div className="btn-row" style={{ flexDirection: "column" }}>
          {!snapshot.hasRolled && (
            <button className="btn" onClick={() => send("ROLL_DICE")}>
              🎲 Gieo xúc xắc
            </button>
          )}
          {me.inJail && (
            <>
              <button className="btn secondary" onClick={() => send("PAY_JAIL_FINE")}>
                Nộp phạt 50 để ra tù
              </button>
              {me.jailFreeCards > 0 && (
                <button className="btn secondary" onClick={() => send("USE_JAIL_CARD")}>
                  Dùng thẻ ra tù ({me.jailFreeCards})
                </button>
              )}
            </>
          )}
          {pendingTile && (
            <>
              <button className="btn" onClick={() => send("BUY_PROPERTY")}>
                Mua {pendingTile.name} ({pendingTile.price})
              </button>
              <button className="btn secondary" onClick={() => send("DECLINE_BUY")}>
                Không mua
              </button>
            </>
          )}
          {buildable.length > 0 && (
            <div className="build-list">
              {buildable.map((o) => (
                <button
                  key={o.tileIndex}
                  className="btn secondary"
                  onClick={() => send("BUILD_HOUSE", o.tileIndex)}
                >
                  🏠 Xây trên {o.tileName} ({snapshot.board[o.tileIndex]?.houseCost})
                </button>
              ))}
            </div>
          )}
          {snapshot.hasRolled && snapshot.pendingBuy == null && (
            <button className="btn secondary" onClick={() => send("END_TURN")}>
              Kết thúc lượt
            </button>
          )}
          <div className="kbd-hint">Phím tắt: R gieo · B mua · E kết thúc</div>
        </div>
      )}
    </div>
  );
}

export function EventLog({ snapshot }: { snapshot: TyPhuSnapshot }) {
  const total = snapshot.log.length;
  const items = [...snapshot.log].reverse();
  return (
    <div className="card">
      <h3>Diễn biến</h3>
      <div className="log">
        {items.map((line, i) => (
          <div key={total - 1 - i} className="slide-in">
            {line}
          </div>
        ))}
      </div>
    </div>
  );
}
