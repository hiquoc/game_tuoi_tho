package com.vietgames.arena.web;

import com.vietgames.arena.platform.GameRoom;
import com.vietgames.arena.platform.RoomManager;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/** Lobby REST API. Gameplay itself runs over WebSocket. */
@RestController
@RequestMapping("/api")
public class LobbyController {

    private final RoomManager rooms;
    private final GameSocketHandler socketHandler;

    public LobbyController(RoomManager rooms, GameSocketHandler socketHandler) {
        this.rooms = rooms;
        this.socketHandler = socketHandler;
    }

    @PostMapping("/rooms")
    public Map<String, String> createRoom(
            @RequestParam String gameCode,
            @RequestParam String hostName) {
        RoomManager.RoomCreated created = rooms.createRoom(gameCode, hostName);
        return Map.of("roomId", created.roomId(), "playerId", created.playerId());
    }

    @PostMapping("/rooms/{roomId}/join")
    public Map<String, String> joinRoom(
            @PathVariable String roomId,
            @RequestParam String name) {
        return Map.of("playerId", rooms.joinRoom(roomId, name));
    }

    @PostMapping("/rooms/{roomId}/start")
    public Map<String, Object> startGame(@PathVariable String roomId) {
        GameRoom room = rooms.get(roomId);
        room.start();
        socketHandler.broadcastRoom(roomId);
        return Map.of("ok", true, "snapshot", room.snapshot());
    }

    @GetMapping("/rooms/{roomId}")
    public Object roomInfo(@PathVariable String roomId) {
        return rooms.get(roomId).snapshot();
    }
}
