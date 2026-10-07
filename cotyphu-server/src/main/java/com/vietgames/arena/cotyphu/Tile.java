package com.vietgames.arena.cotyphu;

/** A board square. Sealed so the engine's tile resolution is exhaustive. */
public sealed interface Tile permits
        Tile.Go, Tile.Ownable, Tile.Tax,
        Tile.Chance, Tile.CommunityChest, Tile.Jail, Tile.GoToJail, Tile.FreeParking {

    int index();

    String name();

    /** Short wire-friendly kind: GO, PROPERTY, STATION, UTILITY, TAX, CHANCE, COMMUNITY, JAIL, GO_TO_JAIL, FREE_PARKING */
    String kind();

    /** Ownable tiles (can be bought and charge rent). */
    non-sealed interface Ownable extends Tile {
        int price();
    }

    record Go(int index) implements Tile {
        public String name() { return "Xuất phát"; }
        public String kind() { return "GO"; }
    }

    record Property(int index, String name, PropertyGroup group, int price, int houseCost) implements Ownable {
        public String kind() { return "PROPERTY"; }
    }

    record Station(int index, String name) implements Ownable {
        public int price() { return 200; }
        public String kind() { return "STATION"; }
    }

    record Utility(int index, String name) implements Ownable {
        public int price() { return 150; }
        public String kind() { return "UTILITY"; }
    }

    record Tax(int index, String name, int amount) implements Tile {
        public String kind() { return "TAX"; }
    }

    record Chance(int index) implements Tile {
        public String name() { return "Cơ hội"; }
        public String kind() { return "CHANCE"; }
    }

    record CommunityChest(int index) implements Tile {
        public String name() { return "Khí vận"; }
        public String kind() { return "COMMUNITY"; }
    }

    /** "Just visiting" — landing here does nothing. */
    record Jail(int index) implements Tile {
        public String name() { return "Thăm tù"; }
        public String kind() { return "JAIL"; }
    }

    record GoToJail(int index) implements Tile {
        public String name() { return "Đi tù"; }
        public String kind() { return "GO_TO_JAIL"; }
    }

    record FreeParking(int index) implements Tile {
        public String name() { return "Bãi đỗ xe miễn phí"; }
        public String kind() { return "FREE_PARKING"; }
    }
}
