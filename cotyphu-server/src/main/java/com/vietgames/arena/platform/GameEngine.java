package com.vietgames.arena.platform;

import java.util.List;

/**
 * The contract every game implements. The engine is a pure state machine:
 * given the current authoritative state and a player action, it returns the
 * new state or throws {@link GameException}. No I/O, no sessions here,
 * which keeps it trivially unit-testable.
 */
public interface GameEngine {

    /** Unique code used in URLs and messages, e.g. "cotyphu", "oanquan". */
    String gameCode();

    /** Builds the initial state once the lobby fills up. */
    GameState newGame(List<PlayerInfo> players);

    /** Applies one validated action. Must be deterministic and side-effect free
     *  except for mutating/returning the new state. */
    GameState applyAction(GameState state, String playerId, GameAction action);

    /** Builds the client-facing view of the state. The default assumes
     *  everything is public; games with hidden info (e.g. Tien Len hands)
     *  override this per player. */
    default Object snapshot(GameState state) {
        return state;
    }

    /** Maps a wire-level action name to a typed action. */
    GameAction parseAction(String action, Integer tileIndex);
}
