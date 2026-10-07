package com.vietgames.arena.platform;

/** Thrown when an action is illegal (wrong turn, bad phase, insufficient funds...).
 *  Sent back to the offending client as an error message; game state is untouched. */
public class GameException extends RuntimeException {
    public GameException(String message) {
        super(message);
    }
}
