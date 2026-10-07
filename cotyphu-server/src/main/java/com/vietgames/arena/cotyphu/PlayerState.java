package com.vietgames.arena.cotyphu;

import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/** Mutable per-player state. The engine is the only writer. */
public class PlayerState {
    public final String id;
    public final String name;
    public int money = 1500;
    public int position = 0;
    public boolean inJail = false;
    public int jailTurns = 0;
    public int jailFreeCards = 0;
    public boolean bankrupt = false;
    /** Owned tile indices. */
    public final Set<Integer> properties = new LinkedHashSet<>();
    /** Tile index -> houses built (0-4 houses, 5 = hotel). */
    public final Map<Integer, Integer> houses = new HashMap<>();

    public PlayerState(String id, String name) {
        this.id = id;
        this.name = name;
    }

    public int housesOn(int tileIndex) {
        return houses.getOrDefault(tileIndex, 0);
    }
}
