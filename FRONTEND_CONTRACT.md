# Frontend contract — Cờ Tỷ Phú backend

Paste this into your AI frontend generator. The backend is authoritative:
**the frontend must never implement game rules, dice, money math, or move
validation.** It only renders snapshots and sends actions. All logic lives in
`cotyphu-server/src/main/java/com/vietgames/arena/cotyphu/TyPhuEngine.java`.

Base URL (dev): `http://localhost:8080`

---

## 1. Lobby flow (REST)

| Method & path | Params | Returns |
|---|---|---|
| `POST /api/rooms` | `?gameCode=cotyphu&hostName=<name>` | `{"roomId":"…","playerId":"…"}` — creator keeps `playerId`, shares `roomId` |
| `POST /api/rooms/{roomId}/join` | `?name=<name>` | `{"playerId":"…"}` |
| `POST /api/rooms/{roomId}/start` | — | `{"ok":true,"snapshot":{…game snapshot…}}` |
| `GET /api/rooms/{roomId}` | — | lobby snapshot (before start) or game snapshot (after start) |

Rules: 2–6 players per room. Nobody can join after the game starts.
`gameCode` for this game is always `cotyphu` (the platform will host more
games later under different codes).

## 2. Gameplay (WebSocket)

Connect: `ws://localhost:8080/ws/arena?roomId=<roomId>&playerId=<playerId>`

**Client → server** (JSON text messages). These are the ONLY legal actions:

| `action` | extra fields | when to show the button |
|---|---|---|
| `ROLL_DICE` | — | your turn, haven't rolled yet |
| `BUY_PROPERTY` | — | `pendingBuy` is not null (you landed on an unowned property) |
| `DECLINE_BUY` | — | same as above (you don't want it) |
| `BUILD_HOUSE` | `"tileIndex": <int>` | anytime it's your turn; server validates ownership, full color set, funds |
| `END_TURN` | — | your turn, after rolling (and after any buy decision) |
| `PAY_JAIL_FINE` | — | you're in jail and it's your turn |
| `USE_JAIL_CARD` | — | you're in jail, it's your turn, and you own a jail-free card |

**Server → client**:

- `{"kind":"snapshot","data":{…}}` — sent once on connect, then broadcast to
  **all** players after every action. Re-render the whole board from this.
- `{"kind":"error","message":"…"}` — the action was illegal (wrong turn,
  insufficient funds, …). Messages are Vietnamese, safe to show to the user.
  No state changed; the last snapshot is still current.

> **Important gap the UI must handle:** starting the game via REST does NOT
> push a snapshot to already-connected WebSocket clients. After anyone calls
> `POST /api/rooms/{roomId}/start`, each client should `GET /api/rooms/{roomId}`
> (or reconnect its WebSocket) to get the first game snapshot.

## 3. Snapshot shapes

**Lobby snapshot** (game not started yet):
```json
{"lobby": true, "roomId": "…", "gameCode": "cotyphu",
 "players": {"<playerId>": "<name>", …}}
```

**Game snapshot** (from `TyPhuSnapshot.java`):
```json
{
  "phase": "IN_PROGRESS",        // or "FINISHED"
  "currentPlayerId": "…",        // whose turn it is
  "dice1": 3, "dice2": 4,       // last roll (0,0 before anyone rolls)
  "hasRolled": true,
  "pendingBuy": 6,               // tile index awaiting buy/decline, or null
  "winnerId": null,              // set when phase == FINISHED
  "log": ["An đổ xúc xắc…", …], // last ≤60 game events, Vietnamese, show as feed
  "players": [
    {"id":"…","name":"An","money":1350,"position":6,
     "inJail":false,"bankrupt":false,"jailFreeCards":1,
     "owned":[{"tileIndex":6,"tileName":"Phố Hàng Bông","houses":2}]}
  ],
  "board": [
    {"index":0,"name":"Xuất phát","kind":"GO","group":null,"price":null,"ownerId":null},
    {"index":1,"name":"Phố Huế","kind":"PROPERTY","group":"Nâu","price":60,"ownerId":"…"},
    …
  ]
}
```

**Tile `kind` values:** `GO`, `PROPERTY`, `STATION`, `UTILITY`, `TAX`,
`CHANCE`, `COMMUNITY`, `JAIL`, `GO_TO_JAIL`, `FREE_PARKING`.
**Property `group` values (Vietnamese, for board coloring):**
`Nâu`, `Xanh nhạt`, `Hồng`, `Cam`, `Đỏ`, `Vàng`, `Xanh lá`, `Xanh dương`.

Notes for the UI:
- Board is 40 tiles, index 0 = Xuất phát. Render order from `board[]` as given.
- `houses`: 0–4 = houses, 5 = hotel.
- Start money 1500, salary 200 for passing Xuất phát.
- All state is public in this game — every player sees the same snapshot.

## 4. Suggested screens

1. **Home**: enter name → create room (shows `roomId` to share) or join by `roomId`.
2. **Lobby**: player list, live-updates when someone joins (re-fetch on an
   interval or keep WS open — any client action re-broadcasts), host presses Start.
3. **Board**: 40-tile grid, player tokens on `position`, owner color strips,
   house/hotel markers, dice display (`dice1`/`dice2`), money panel, event log,
   action buttons enabled per the table in section 2. On `FINISHED`, show
   `winnerId`'s name as champion.

## 5. What NOT to build in the frontend

No dice RNG, no money arithmetic, no turn logic, no "can I buy this?" checks —
send the action and let the server's error message tell the user why not.
The backend already validates turn order, ownership, funds, and jail rules.
