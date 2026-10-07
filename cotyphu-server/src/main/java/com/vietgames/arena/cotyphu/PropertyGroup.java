package com.vietgames.arena.cotyphu;

/** Color groups. Owning every street in a group doubles base rent and unlocks building. */
public enum PropertyGroup {
    BROWN("Nâu"),
    LIGHT_BLUE("Xanh nhạt"),
    PINK("Hồng"),
    ORANGE("Cam"),
    RED("Đỏ"),
    YELLOW("Vàng"),
    GREEN("Xanh lá"),
    DARK_BLUE("Xanh dương");

    private final String displayName;

    PropertyGroup(String displayName) {
        this.displayName = displayName;
    }

    public String displayName() {
        return displayName;
    }
}
