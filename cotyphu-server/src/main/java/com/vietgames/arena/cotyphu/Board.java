package com.vietgames.arena.cotyphu;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import static com.vietgames.arena.cotyphu.PropertyGroup.*;

/** The classic 40-tile board, localized with Vietnamese street names. */
public final class Board {

    public static final int SIZE = 40;
    public static final int JAIL_INDEX = 10;

    public static final List<Tile> TILES = build();

    private static final Map<PropertyGroup, List<Integer>> GROUP_TILES = new EnumMap<>(PropertyGroup.class);

    static {
        for (Tile t : TILES) {
            if (t instanceof Tile.Property p) {
                GROUP_TILES.computeIfAbsent(p.group(), g -> new ArrayList<>()).add(p.index());
            }
        }
    }

    private Board() {
    }

    public static Tile get(int index) {
        return TILES.get(index);
    }

    /** All tile indices belonging to a color group. */
    public static List<Integer> tilesOfGroup(PropertyGroup group) {
        return GROUP_TILES.getOrDefault(group, List.of());
    }

    private static List<Tile> build() {
        List<Tile> t = new ArrayList<>(SIZE);
        t.add(new Tile.Go(0));
        t.add(new Tile.Property(1, "Phố Huế", BROWN, 60, 50));
        t.add(new Tile.CommunityChest(2));
        t.add(new Tile.Property(3, "Phố Bạch Mai", BROWN, 60, 50));
        t.add(new Tile.Tax(4, "Thuế thu nhập", 200));
        t.add(new Tile.Station(5, "Ga Hà Nội"));
        t.add(new Tile.Property(6, "Phố Hàng Bông", LIGHT_BLUE, 100, 50));
        t.add(new Tile.Chance(7));
        t.add(new Tile.Property(8, "Phố Hàng Gai", LIGHT_BLUE, 100, 50));
        t.add(new Tile.Property(9, "Phố Hàng Đào", LIGHT_BLUE, 120, 50));
        t.add(new Tile.Jail(10));
        t.add(new Tile.Property(11, "Phố Tràng Tiền", PINK, 140, 100));
        t.add(new Tile.Utility(12, "Công ty Điện lực"));
        t.add(new Tile.Property(13, "Phố Đinh Tiên Hoàng", PINK, 140, 100));
        t.add(new Tile.Property(14, "Phố Lý Thái Tổ", PINK, 160, 100));
        t.add(new Tile.Station(15, "Ga Sài Gòn"));
        t.add(new Tile.Property(16, "Phố Nguyễn Huệ", ORANGE, 180, 100));
        t.add(new Tile.CommunityChest(17));
        t.add(new Tile.Property(18, "Phố Đồng Khởi", ORANGE, 180, 100));
        t.add(new Tile.Property(19, "Phố Lê Lợi", ORANGE, 200, 100));
        t.add(new Tile.FreeParking(20));
        t.add(new Tile.Property(21, "Phố Trần Hưng Đạo", RED, 220, 150));
        t.add(new Tile.Chance(22));
        t.add(new Tile.Property(23, "Phố Lý Thường Kiệt", RED, 220, 150));
        t.add(new Tile.Property(24, "Phố Hai Bà Trưng", RED, 240, 150));
        t.add(new Tile.Station(25, "Ga Đà Nẵng"));
        t.add(new Tile.Property(26, "Phố Phạm Ngũ Lão", YELLOW, 260, 150));
        t.add(new Tile.Property(27, "Phố Bùi Viện", YELLOW, 260, 150));
        t.add(new Tile.Utility(28, "Công ty Cấp nước"));
        t.add(new Tile.Property(29, "Phố Đề Thám", YELLOW, 280, 150));
        t.add(new Tile.GoToJail(30));
        t.add(new Tile.Property(31, "Phố Võ Thị Sáu", GREEN, 300, 200));
        t.add(new Tile.Property(32, "Phố Nam Kỳ Khởi Nghĩa", GREEN, 300, 200));
        t.add(new Tile.CommunityChest(33));
        t.add(new Tile.Property(34, "Phố Pasteur", GREEN, 320, 200));
        t.add(new Tile.Station(35, "Ga Huế"));
        t.add(new Tile.Chance(36));
        t.add(new Tile.Property(37, "Phố Điện Biên Phủ", DARK_BLUE, 350, 200));
        t.add(new Tile.Tax(38, "Thuế xa xỉ", 100));
        t.add(new Tile.Property(39, "Phố Võ Văn Tần", DARK_BLUE, 400, 200));
        return List.copyOf(t);
    }
}
