package com.vietgames.arena.cotyphu;

import java.util.List;

/**
 * Client-facing view of a Co Ty Phu game. In Monopoly everything is public
 * information, so every player receives the same snapshot.
 */
public record TyPhuSnapshot(
        String phase,
        List<PlayerView> players,
        String currentPlayerId,
        int dice1,
        int dice2,
        boolean hasRolled,
        Integer pendingBuy,
        String winnerId,
        List<String> log,
        List<TileView> board) {

    public record PlayerView(
            String id,
            String name,
            int money,
            int position,
            boolean inJail,
            boolean bankrupt,
            int jailFreeCards,
            List<OwnedView> owned) {
    }

    public record OwnedView(int tileIndex, String tileName, int houses) {
    }

    public record TileView(
            int index,
            String name,
            String kind,
            String group,
            Integer price,
            Integer houseCost,
            String ownerId) {
    }
}
