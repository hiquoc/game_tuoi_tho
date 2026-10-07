package com.vietgames.arena.web;

/** One line from the client: which action, plus the tile for BUILD_HOUSE. */
public record ClientMessage(String action, Integer tileIndex) {
}
