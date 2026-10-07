# Viet Games Arena — Sân chơi game tuổi thơ 🇻🇳

A multiplayer web platform for Vietnamese childhood games, starting with **Cờ Tỷ Phú** (Vietnamese Monopoly).
The backend is authoritative: the server owns all game state, and clients only render snapshots and send actions.

## Project layout

```
viet-games-arena/
└── cotyphu-server/          # Spring Boot backend (Java 21)
    └── src/main/java/com/vietgames/arena/
        ├── platform/        # Game-agnostic core (reused by every future game)
        │   ├── GameEngine.java    # State-machine contract: newGame / applyAction / snapshot / parseAction
        │   ├── GameRoom.java      # One live game: lobby, state, WebSocket sessions (synchronized)
        │   └── RoomManager.java   # Engine registry + room registry
        ├── cotyphu/         # Cờ Tỷ Phú implementation
        │   ├── Board.java         # 40-tile board with Vietnamese street names
        │   ├── Tile.java          # Sealed tile hierarchy (Property, Station, Utility, Tax, Chance, ...)
        │   ├── TyPhuEngine.java   # All rules: turns, rent, houses, jail, cards, bankruptcy
        │   ├── TyPhuState.java    # Authoritative state
        │   └── TyPhuSnapshot.java # Client-facing DTO
        └── web/
            ├── LobbyController.java  # REST: create/join/start room
            └── GameSocketHandler.java# WebSocket: /ws/arena?roomId=..&playerId=..
```

## Why this architecture

- **Authoritative server.** Dice, cards, rent math and win detection all run in `TyPhuEngine`.
  A hacked client can never grant itself money or teleport — it can only send the 7 legal actions.
- **Dumb client.** The server broadcasts a full snapshot after every action, so the frontend
  (AI-generated) holds no game logic and can't desync.
- **Engine is a pure state machine.** No I/O inside `TyPhuEngine`, which makes the rules
  trivially unit-testable with fixed dice (`DiceRoller.FixedDiceRoller`).
- **Platform seam.** The next game (Ô Ăn Quan, Cờ Cá Ngựa, Tiến Lên) only needs a new
  `GameEngine` implementation + package; rooms, lobby and sockets are reused untouched.

## Rules implemented (milestone 1)

- 2–6 players, 1500 starting money, 200 salary for passing Xuất phát
- Roll/move/buy, rent (streets, stations 25·2ⁿ, utilities 4×/10× dice), full color set doubles base rent
- Houses & hotels (up to 5 levels), jail (3 doubles, 3 failed rolls, fine 50, jail-free cards)
- Chance / Khí vận decks (8 cards each), taxes, bankruptcy with asset transfer, win detection
- Out of scope for now: auctions, trading, mortgages (see Roadmap)

## Run it

Requires JDK 21 and Maven.

```bash
cd cotyphu-server
mvn spring-boot:run
```

Lobby flow:

```bash
# create a room (returns roomId + playerId)
curl -X POST "http://localhost:8080/api/rooms?gameCode=cotyphu&hostName=An"

# join
curl -X POST "http://localhost:8080/api/rooms/<roomId>/join?name=Binh"

# start the game
curl -X POST "http://localhost:8080/api/rooms/<roomId>/start"
```

Then connect a WebSocket to `ws://localhost:8080/ws/arena?roomId=<roomId>&playerId=<playerId>`
and send actions like `{"action":"ROLL_DICE"}`, `{"action":"BUY_PROPERTY"}`,
`{"action":"BUILD_HOUSE","tileIndex":3}`, `{"action":"END_TURN"}`.
The server replies with `{"kind":"snapshot","data":{...}}` after every action,
or `{"kind":"error","message":"..."}` when an action is illegal.

## Tests

```bash
cd cotyphu-server
mvn test
```

Covers: buying, rent, doubled rent on full sets, 3-doubles jail, bankruptcy → win,
out-of-turn rejection, buy-decision gating, GO salary, house building rules.

## Roadmap

- [ ] Frontend (Next.js): board render, dice animation, lobby UI — AI-generated, logic-free
- [ ] Auctions on declined purchases, player trading, mortgages
- [ ] Game 2: Ô Ăn Quan (fully visible state — simplest next engine)
- [ ] Game 3: Cờ Cá Ngựa
- [ ] Game 4: Tiến Lên — per-player snapshots for hidden hands, the real anti-cheat showcase
- [ ] Matchmaking queue + reconnect with session tokens
- [ ] Load test the socket layer (how many concurrent rooms on one instance?)
