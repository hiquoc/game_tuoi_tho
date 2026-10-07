package com.vietgames.arena.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vietgames.arena.platform.GameAction;
import com.vietgames.arena.platform.GameException;
import com.vietgames.arena.platform.GameRoom;
import com.vietgames.arena.platform.RoomManager;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.net.URI;
import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Single WebSocket endpoint for all games: /ws/arena?roomId=...&playerId=...
 *
 * Client -> server: {"action":"ROLL_DICE"} (see {@link ClientMessage})
 * Server -> client: {"kind":"snapshot","data":{...}} or {"kind":"error","message":"..."}
 */
@Component
public class GameSocketHandler extends TextWebSocketHandler {

    private final RoomManager rooms;
    private final ObjectMapper mapper;

    public GameSocketHandler(RoomManager rooms, ObjectMapper mapper) {
        this.rooms = rooms;
        this.mapper = mapper;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        Map<String, String> q = queryParams(session);
        try {
            GameRoom room = rooms.get(q.get("roomId"));
            String playerId = q.get("playerId");
            room.addSession(playerId, session);
            send(session, Map.of("kind", "snapshot", "data", room.snapshot()));
        } catch (GameException e) {
            send(session, Map.of("kind", "error", "message", e.getMessage()));
            session.close(CloseStatus.POLICY_VIOLATION);
        }
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        Map<String, String> q = queryParams(session);
        GameRoom room = rooms.get(q.get("roomId"));
        String playerId = q.get("playerId");
        try {
            ClientMessage cm = mapper.readValue(message.getPayload(), ClientMessage.class);
            GameAction action = room.getEngine().parseAction(cm.action(), cm.tileIndex());
            Object snapshot = room.apply(playerId, action);
            broadcast(room, Map.of("kind", "snapshot", "data", snapshot));
        } catch (GameException e) {
            send(session, Map.of("kind", "error", "message", e.getMessage()));
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        try {
            Map<String, String> q = queryParams(session);
            String roomId = q.get("roomId");
            String playerId = q.get("playerId");
            GameRoom room = rooms.get(roomId);
            if (!room.isStarted()) {
                // Lobby cleanup: a disconnected client leaves the lobby so the
                // player list (and a future game) never gets stuck on ghosts.
                if (room.removePlayer(playerId)) {
                    broadcastRoom(roomId);
                }
            } else {
                room.removeSession(playerId);
            }
        } catch (Exception ignored) {
            // room already gone; nothing to clean up
        }
    }

    private void broadcast(GameRoom room, Object payload) throws Exception {
        String json = mapper.writeValueAsString(payload);
        TextMessage msg = new TextMessage(json);
        for (WebSocketSession s : room.sessions()) {
            if (s.isOpen()) {
                s.sendMessage(msg);
            }
        }
    }

    /** Pushes the current snapshot to every connected client in the room.
     *  Used when the game starts (REST) so all players move to the board together. */
    public void broadcastRoom(String roomId) {
        try {
            GameRoom room = rooms.get(roomId);
            broadcast(room, Map.of("kind", "snapshot", "data", room.snapshot()));
        } catch (Exception ignored) {
            // best effort: lobby polling covers any stragglers
        }
    }

    private void send(WebSocketSession session, Object payload) throws Exception {
        session.sendMessage(new TextMessage(mapper.writeValueAsString(payload)));
    }

    private Map<String, String> queryParams(WebSocketSession session) {
        URI uri = session.getUri();
        if (uri == null || uri.getQuery() == null) {
            return Map.of();
        }
        return Arrays.stream(uri.getQuery().split("&"))
                .map(p -> p.split("=", 2))
                .filter(p -> p.length == 2)
                .collect(Collectors.toMap(p -> p[0], p -> p[1]));
    }
}
