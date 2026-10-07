package com.vietgames.arena.platform;

import org.springframework.web.socket.WebSocketSession;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * One running game instance: lobby players, the engine, the authoritative
 * state, and the connected WebSocket sessions. All state transitions go
 * through synchronized methods so concurrent messages can't corrupt a game.
 */
public class GameRoom {

    private final String roomId;
    private final GameEngine engine;
    private final Map<String, String> playerNames = new LinkedHashMap<>();
    private final Map<String, WebSocketSession> sessions = new ConcurrentHashMap<>();
    private GameState state;

    public GameRoom(String roomId, GameEngine engine) {
        this.roomId = roomId;
        this.engine = engine;
    }

    public String getRoomId() {
        return roomId;
    }

    public GameEngine getEngine() {
        return engine;
    }

    public synchronized String addPlayer(String name) {
        if (state != null) {
            throw new GameException("Ván đã bắt đầu, không thể vào thêm");
        }
        if (playerNames.size() >= 6) {
            throw new GameException("Phòng đã đầy (tối đa 6 người)");
        }
        String id = UUID.randomUUID().toString().substring(0, 8);
        playerNames.put(id, name);
        return id;
    }

    public synchronized void start() {
        if (state != null) {
            throw new GameException("Ván đã bắt đầu rồi");
        }
        List<PlayerInfo> infos = playerNames.entrySet().stream()
                .map(e -> new PlayerInfo(e.getKey(), e.getValue()))
                .toList();
        state = engine.newGame(infos);
    }

    public synchronized boolean isStarted() {
        return state != null;
    }

    /** Removes a player from the lobby. Only allowed before the game starts.
     *  Used for explicit leaves and for cleaning up disconnected lobby clients. */
    public synchronized boolean removePlayer(String playerId) {
        if (state != null) {
            return false;
        }
        sessions.remove(playerId);
        return playerNames.remove(playerId) != null;
    }

    /** Applies an action and returns the fresh snapshot for broadcast. */
    public synchronized Object apply(String playerId, GameAction action) {
        if (state == null) {
            throw new GameException("Ván chưa bắt đầu");
        }
        if (!playerNames.containsKey(playerId)) {
            throw new GameException("Người chơi không thuộc phòng này");
        }
        state = engine.applyAction(state, playerId, action);
        return engine.snapshot(state);
    }

    public synchronized Object snapshot() {
        if (state == null) {
            return Map.of(
                    "lobby", true,
                    "roomId", roomId,
                    "gameCode", engine.gameCode(),
                    "players", playerNames);
        }
        return engine.snapshot(state);
    }

    public void addSession(String playerId, WebSocketSession session) {
        sessions.put(playerId, session);
    }

    public void removeSession(String playerId) {
        sessions.remove(playerId);
    }

    public Collection<WebSocketSession> sessions() {
        return sessions.values();
    }
}
