package com.vietgames.arena.platform;

import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/** Registry of engines and live rooms. */
@Service
public class RoomManager {

    private final Map<String, GameEngine> engines;
    private final Map<String, GameRoom> rooms = new ConcurrentHashMap<>();

    public RoomManager(List<GameEngine> engineList) {
        this.engines = engineList.stream()
                .collect(Collectors.toMap(GameEngine::gameCode, e -> e));
    }

    public record RoomCreated(String roomId, String playerId) {
    }

    public RoomCreated createRoom(String gameCode, String hostName) {
        GameEngine engine = engines.get(gameCode);
        if (engine == null) {
            throw new GameException("Game không tồn tại: " + gameCode);
        }
        String roomId = UUID.randomUUID().toString().substring(0, 8);
        GameRoom room = new GameRoom(roomId, engine);
        String playerId = room.addPlayer(hostName);
        rooms.put(roomId, room);
        return new RoomCreated(roomId, playerId);
    }

    public String joinRoom(String roomId, String name) {
        return get(roomId).addPlayer(name);
    }

    public GameRoom get(String roomId) {
        GameRoom room = rooms.get(roomId);
        if (room == null) {
            throw new GameException("Phòng không tồn tại: " + roomId);
        }
        return room;
    }
}
