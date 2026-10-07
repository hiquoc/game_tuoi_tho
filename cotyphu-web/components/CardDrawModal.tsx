"use client";

import { useEffect } from "react";
import Avatar from "./Avatar";
import { CardDraw } from "../hooks/useCardDraw";

/** Animated popup shown to every player when someone draws a card. */
export default function CardDrawModal({
  draw,
  onClose,
}: {
  draw: CardDraw | null;
  onClose: () => void;
}) {
  useEffect(() => {
    if (!draw) return;
    const t = setTimeout(onClose, 6000);
    return () => clearTimeout(t);
  }, [draw, onClose]);

  if (!draw) return null;
  const isChance = draw.deck === "chance";

  return (
    <div className="modal-overlay" onClick={onClose}>
      <div
        key={draw.key}
        className={`game-card ${draw.deck} flip-in`}
        onClick={(e) => e.stopPropagation()}
      >
        <img
          src={isChance ? "/cards/chance-back.png" : "/cards/community-back.png"}
          alt=""
          className="game-card-art"
        />
        <div className="game-card-deck">{isChance ? "CƠ HỘI" : "KHÍ VẬN"}</div>
        <div className="game-card-text">{draw.text}</div>
        <div className="game-card-player">
          <Avatar index={draw.playerIndex} name={draw.playerName} size={30} />
          <span>{draw.playerName} đã rút thẻ</span>
        </div>
        <div className="game-card-hint">Nhấn để đóng</div>
      </div>
    </div>
  );
}
