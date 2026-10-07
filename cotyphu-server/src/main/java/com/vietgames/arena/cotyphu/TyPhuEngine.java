package com.vietgames.arena.cotyphu;

import com.vietgames.arena.platform.GameAction;
import com.vietgames.arena.platform.GameEngine;
import com.vietgames.arena.platform.GameException;
import com.vietgames.arena.platform.GameState;
import com.vietgames.arena.platform.PlayerInfo;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

/**
 * Authoritative rules engine for Co Ty Phu.
 *
 * The client is dumb: it renders snapshots and sends one of the seven
 * {@link TyPhuAction}s. Everything else — dice, movement, rent, cards,
 * jail, bankruptcy, win detection — happens here, so a hacked client
 * can never grant itself money or teleport.
 */
@Component
public class TyPhuEngine implements GameEngine {

    public static final String GAME_CODE = "cotyphu";
    static final int GO_SALARY = 200;
    static final int JAIL_FINE = 50;
    static final int START_MONEY = 1500;

    private final DiceRoller dice;

    public TyPhuEngine() {
        this(new DiceRoller.RandomDiceRoller());
    }

    /** Test constructor with deterministic dice. */
    public TyPhuEngine(DiceRoller dice) {
        this.dice = dice;
    }

    @Override
    public String gameCode() {
        return GAME_CODE;
    }

    @Override
    public GameState newGame(List<PlayerInfo> players) {
        if (players.size() < 2 || players.size() > 6) {
            throw new GameException("Cờ tỷ phú cần 2-6 người chơi");
        }
        TyPhuState s = new TyPhuState();
        for (PlayerInfo p : players) {
            s.players.add(new PlayerState(p.id(), p.name()));
        }
        s.initDecks();
        s.log("Ván mới bắt đầu! " + players.size() + " người chơi, mỗi người " + START_MONEY + ".");
        s.log("— Lượt của " + s.currentPlayer().name + " —");
        return s;
    }

    @Override
    public GameState applyAction(GameState gs, String playerId, GameAction ga) {
        TyPhuState s = (TyPhuState) gs;
        TyPhuAction action = (TyPhuAction) ga;
        if (s.phase != TyPhuState.Phase.IN_PROGRESS) {
            throw new GameException("Ván chưa bắt đầu hoặc đã kết thúc");
        }
        PlayerState me = findPlayer(s, playerId);
        if (me.bankrupt) {
            throw new GameException("Bạn đã phá sản");
        }
        if (!s.currentPlayer().id.equals(playerId)) {
            throw new GameException("Chưa tới lượt bạn");
        }
        switch (action) {
            case TyPhuAction.RollDice r -> doRoll(s, me);
            case TyPhuAction.BuyProperty b -> doBuy(s, me);
            case TyPhuAction.DeclineBuy d -> {
                requirePendingBuy(s);
                s.log(me.name + " không mua " + Board.get(s.pendingBuy).name() + ".");
                s.pendingBuy = null;
            }
            case TyPhuAction.BuildHouse b -> doBuild(s, me, b.tileIndex());
            case TyPhuAction.EndTurn e -> doEndTurn(s, me);
            case TyPhuAction.PayJailFine p -> doPayJailFine(s, me);
            case TyPhuAction.UseJailCard u -> doUseJailCard(s, me);
        }
        return s;
    }

    @Override
    public GameAction parseAction(String action, Integer tileIndex) {
        return switch (action) {
            case "ROLL_DICE" -> new TyPhuAction.RollDice();
            case "BUY_PROPERTY" -> new TyPhuAction.BuyProperty();
            case "DECLINE_BUY" -> new TyPhuAction.DeclineBuy();
            case "BUILD_HOUSE" -> new TyPhuAction.BuildHouse(tileIndex == null ? -1 : tileIndex);
            case "END_TURN" -> new TyPhuAction.EndTurn();
            case "PAY_JAIL_FINE" -> new TyPhuAction.PayJailFine();
            case "USE_JAIL_CARD" -> new TyPhuAction.UseJailCard();
            default -> throw new GameException("Action không hợp lệ: " + action);
        };
    }

    @Override
    public Object snapshot(GameState gs) {
        TyPhuState s = (TyPhuState) gs;
        List<TyPhuSnapshot.TileView> board = new ArrayList<>();
        for (Tile t : Board.TILES) {
            String owner = (t instanceof Tile.Ownable) ? ownerOf(s, t.index()) : null;
            String group = (t instanceof Tile.Property p) ? p.group().displayName() : null;
            Integer price = (t instanceof Tile.Ownable o) ? o.price() : null;
            board.add(new TyPhuSnapshot.TileView(t.index(), t.name(), t.kind(), group, price, owner));
        }
        List<TyPhuSnapshot.PlayerView> players = s.players.stream().map(p ->
                new TyPhuSnapshot.PlayerView(
                        p.id, p.name, p.money, p.position, p.inJail, p.bankrupt, p.jailFreeCards,
                        p.properties.stream().sorted()
                                .map(i -> new TyPhuSnapshot.OwnedView(i, Board.get(i).name(), p.housesOn(i)))
                                .toList()))
                .toList();
        return new TyPhuSnapshot(
                s.phase.name(), players, s.currentPlayer().id,
                s.dice1, s.dice2, s.hasRolled, s.pendingBuy,
                s.winnerId, List.copyOf(s.log), board);
    }

    // ------------------------------------------------------------------
    // Turn flow
    // ------------------------------------------------------------------

    private void doRoll(TyPhuState s, PlayerState me) {
        if (s.hasRolled) {
            throw new GameException("Bạn đã gieo xúc xắc rồi");
        }
        int d1 = dice.roll();
        int d2 = dice.roll();
        s.dice1 = d1;
        s.dice2 = d2;
        boolean doubles = d1 == d2;

        if (me.inJail) {
            s.hasRolled = true;
            if (doubles) {
                releaseFromJail(s, me);
                s.log(me.name + " gieo được đôi, ra tù!");
                moveAndResolve(s, me, d1 + d2);
            } else {
                me.jailTurns++;
                s.log(me.name + " gieo " + d1 + "+" + d2 + " (lần " + me.jailTurns + "/3 trong tù)");
                if (me.jailTurns >= 3) {
                    s.log(me.name + " hết 3 lượt, nộp phạt " + JAIL_FINE + " để ra tù");
                    payToBank(s, me, JAIL_FINE, "tiền phạt tù");
                    if (!me.bankrupt) {
                        releaseFromJail(s, me);
                        moveAndResolve(s, me, d1 + d2);
                    }
                }
            }
            return;
        }

        if (doubles) {
            s.doublesCount++;
            if (s.doublesCount >= 3) {
                s.log(me.name + " ra đôi 3 lần liên tiếp!");
                sendToJail(s, me);
                return;
            }
            s.hasRolled = false; // doubles: roll again
            s.log(me.name + " gieo đôi " + d1 + "+" + d2 + ", được gieo tiếp");
        } else {
            s.doublesCount = 0;
            s.hasRolled = true;
        }
        moveAndResolve(s, me, d1 + d2);
    }

    private void doBuy(TyPhuState s, PlayerState me) {
        requirePendingBuy(s);
        Tile tile = Board.get(s.pendingBuy);
        if (!(tile instanceof Tile.Ownable ownable)) {
            throw new GameException("Ô này không mua được");
        }
        if (ownerOf(s, tile.index()) != null) {
            throw new GameException("Ô này đã có chủ");
        }
        if (me.money < ownable.price()) {
            throw new GameException("Không đủ tiền (cần " + ownable.price() + ")");
        }
        me.money -= ownable.price();
        me.properties.add(tile.index());
        s.log(me.name + " mua " + tile.name() + " giá " + ownable.price());
        s.pendingBuy = null;
    }

    private void doBuild(TyPhuState s, PlayerState me, int tileIndex) {
        Tile tile = tileOrThrow(tileIndex);
        if (!(tile instanceof Tile.Property p)) {
            throw new GameException("Chỉ xây nhà trên đất");
        }
        if (!me.properties.contains(tileIndex)) {
            throw new GameException("Đất này không phải của bạn");
        }
        if (!ownsFullSet(s, me, p.group())) {
            throw new GameException("Cần sở hữu cả cụm màu " + p.group().displayName());
        }
        int houses = me.housesOn(tileIndex);
        if (houses >= 5) {
            throw new GameException("Đã có khách sạn rồi");
        }
        if (me.money < p.houseCost()) {
            throw new GameException("Không đủ tiền xây (cần " + p.houseCost() + ")");
        }
        me.money -= p.houseCost();
        me.houses.put(tileIndex, houses + 1);
        s.log(me.name + " xây " + (houses + 1 == 5 ? "KHÁCH SẠN" : "nhà cấp " + (houses + 1)) + " trên " + p.name());
    }

    private void doEndTurn(TyPhuState s, PlayerState me) {
        if (s.pendingBuy != null) {
            throw new GameException("Hãy quyết định mua/không mua ô đất trước");
        }
        s.hasRolled = false;
        s.doublesCount = 0;
        s.dice1 = 0;
        s.dice2 = 0;
        do {
            s.currentPlayerIdx = (s.currentPlayerIdx + 1) % s.players.size();
        } while (s.currentPlayer().bankrupt);
        s.log("— Lượt của " + s.currentPlayer().name + " —");
    }

    private void doPayJailFine(TyPhuState s, PlayerState me) {
        if (!me.inJail) {
            throw new GameException("Bạn không ở trong tù");
        }
        payToBank(s, me, JAIL_FINE, "tiền phạt ra tù");
        if (!me.bankrupt) {
            releaseFromJail(s, me);
            s.log(me.name + " nộp phạt, ra tù");
        }
        s.hasRolled = true;
    }

    private void doUseJailCard(TyPhuState s, PlayerState me) {
        if (!me.inJail) {
            throw new GameException("Bạn không ở trong tù");
        }
        if (me.jailFreeCards <= 0) {
            throw new GameException("Bạn không có thẻ ra tù");
        }
        me.jailFreeCards--;
        releaseFromJail(s, me);
        s.log(me.name + " dùng thẻ ra tù miễn phí");
        s.hasRolled = true;
    }

    // ------------------------------------------------------------------
    // Movement & tile resolution
    // ------------------------------------------------------------------

    private void moveAndResolve(TyPhuState s, PlayerState me, int steps) {
        int old = me.position;
        if (old + steps >= Board.SIZE) {
            me.money += GO_SALARY;
            s.log(me.name + " đi qua Xuất phát, +" + GO_SALARY);
        }
        me.position = (old + steps) % Board.SIZE;
        Tile tile = Board.get(me.position);
        s.log(me.name + " dừng ở " + tile.name());
        resolveTile(s, me, tile, steps);
    }

    private void resolveTile(TyPhuState s, PlayerState me, Tile tile, int diceSum) {
        switch (tile) {
            case Tile.Property p -> resolveOwnable(s, me, p);
            case Tile.Station st -> {
                String ownerId = ownerOf(s, st.index());
                if (ownerId == null) {
                    offerBuy(s, me, st);
                } else if (!ownerId.equals(me.id)) {
                    int owned = countOwned(s, findPlayer(s, ownerId), Tile.Station.class);
                    payToPlayer(s, me, findPlayer(s, ownerId), 25 * (1 << (owned - 1)), "tiền ga " + st.name());
                }
            }
            case Tile.Utility u -> {
                String ownerId = ownerOf(s, u.index());
                if (ownerId == null) {
                    offerBuy(s, me, u);
                } else if (!ownerId.equals(me.id)) {
                    int owned = countOwned(s, findPlayer(s, ownerId), Tile.Utility.class);
                    payToPlayer(s, me, findPlayer(s, ownerId), diceSum * (owned == 2 ? 10 : 4), "tiền điện nước");
                }
            }
            case Tile.Tax t -> payToBank(s, me, t.amount(), "thuế (" + t.name() + ")");
            case Tile.Chance c -> drawCard(s, me, s.chanceDeck);
            case Tile.CommunityChest c -> drawCard(s, me, s.communityDeck);
            case Tile.GoToJail g -> {
                s.log(me.name + " bị bắt vào tù!");
                sendToJail(s, me);
            }
            default -> { /* GO, JAIL (visiting), FREE_PARKING: nothing */ }
        }
    }

    private void resolveOwnable(TyPhuState s, PlayerState me, Tile.Property p) {
        String ownerId = ownerOf(s, p.index());
        if (ownerId == null) {
            offerBuy(s, me, p);
        } else if (!ownerId.equals(me.id)) {
            PlayerState owner = findPlayer(s, ownerId);
            payToPlayer(s, me, owner, propertyRent(s, owner, p), "tiền thuê " + p.name());
        } else {
            s.log(me.name + " về ô đất của mình");
        }
    }

    private void offerBuy(TyPhuState s, PlayerState me, Tile.Ownable tile) {
        s.pendingBuy = tile.index();
        s.log("Ô trống! " + me.name + " có thể mua " + tile.name() + " giá " + tile.price());
    }

    /** Rent for a street: house table, or doubled base rent on a full set. */
    static int propertyRent(TyPhuState s, PlayerState owner, Tile.Property p) {
        int houses = owner.housesOn(p.index());
        int base = Math.max(1, p.price() / 30);
        if (houses > 0) {
            int[] mult = {1, 5, 15, 45, 80, 125};
            return base * mult[houses];
        }
        return ownsFullSet(s, owner, p.group()) ? base * 2 : base;
    }

    private void drawCard(TyPhuState s, PlayerState me, Deque<Card> deck) {
        Card card = deck.pollFirst();
        deck.addLast(card); // recycle
        s.log(me.name + " rút thẻ: " + card.text());
        switch (card.kind()) {
            case ADVANCE_GO -> {
                me.position = 0;
                me.money += GO_SALARY;
                s.log(me.name + " tiến tới Xuất phát, +" + GO_SALARY);
            }
            case GO_TO_JAIL -> sendToJail(s, me);
            case BACK_3 -> {
                me.position = (me.position + Board.SIZE - 3) % Board.SIZE;
                s.log(me.name + " lùi 3 ô tới " + Board.get(me.position).name());
                resolveTile(s, me, Board.get(me.position), 0);
            }
            case PAY_BANK -> payToBank(s, me, card.value(), "thẻ phạt");
            case RECEIVE_BANK -> {
                me.money += card.value();
                s.log(me.name + " nhận " + card.value());
            }
            case JAIL_FREE -> {
                me.jailFreeCards++;
                s.log(me.name + " giữ 1 thẻ ra tù miễn phí");
            }
            case GIFT_FROM_ALL -> {
                for (PlayerState other : s.players) {
                    if (!other.id.equals(me.id) && !other.bankrupt) {
                        payToPlayer(s, other, me, card.value(), "quà tặng " + me.name);
                        if (me.bankrupt) break;
                    }
                }
            }
        }
    }

    // ------------------------------------------------------------------
    // Money, jail, bankruptcy
    // ------------------------------------------------------------------

    private void payToBank(TyPhuState s, PlayerState from, int amount, String reason) {
        from.money -= amount;
        s.log(from.name + " trả " + amount + " " + reason);
        if (from.money < 0) {
            bankrupt(s, from, null);
        }
    }

    private void payToPlayer(TyPhuState s, PlayerState from, PlayerState to, int amount, String reason) {
        from.money -= amount;
        to.money += amount;
        s.log(from.name + " trả " + amount + " cho " + to.name + " (" + reason + ")");
        if (from.money < 0) {
            bankrupt(s, from, to);
        }
    }

    private void bankrupt(TyPhuState s, PlayerState p, PlayerState creditor) {
        p.bankrupt = true;
        s.log("💸 " + p.name + " PHÁ SẢN!");
        if (creditor != null) {
            for (int tile : p.properties) {
                creditor.properties.add(tile);
                int h = p.housesOn(tile);
                if (h > 0) creditor.houses.put(tile, h);
            }
            s.log("Tài sản của " + p.name + " chuyển cho " + creditor.name);
        } else {
            s.log("Tài sản của " + p.name + " về lại ngân hàng");
        }
        p.properties.clear();
        p.houses.clear();
        p.money = 0;
        if (p.inJail) {
            p.inJail = false;
        }
        checkWinner(s);
    }

    private void checkWinner(TyPhuState s) {
        List<PlayerState> alive = s.players.stream().filter(p -> !p.bankrupt).toList();
        if (alive.size() == 1) {
            s.phase = TyPhuState.Phase.FINISHED;
            s.winnerId = alive.get(0).id;
            s.log("🏆 " + alive.get(0).name + " THẮNG!");
        }
    }

    private void sendToJail(TyPhuState s, PlayerState me) {
        me.position = Board.JAIL_INDEX;
        me.inJail = true;
        me.jailTurns = 0;
        s.doublesCount = 0;
        s.hasRolled = true;
        s.pendingBuy = null;
        s.log(me.name + " vào tù");
    }

    private void releaseFromJail(TyPhuState s, PlayerState me) {
        me.inJail = false;
        me.jailTurns = 0;
    }

    // ------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------

    private PlayerState findPlayer(TyPhuState s, String playerId) {
        return s.players.stream()
                .filter(p -> p.id.equals(playerId))
                .findFirst()
                .orElseThrow(() -> new GameException("Không tìm thấy người chơi"));
    }

    private String ownerOf(TyPhuState s, int tileIndex) {
        for (PlayerState p : s.players) {
            if (!p.bankrupt && p.properties.contains(tileIndex)) {
                return p.id;
            }
        }
        return null;
    }

    static boolean ownsFullSet(TyPhuState s, PlayerState p, PropertyGroup group) {
        return p.properties.containsAll(Board.tilesOfGroup(group));
    }

    private int countOwned(TyPhuState s, PlayerState p, Class<? extends Tile> type) {
        int n = 0;
        for (int i : p.properties) {
            if (type.isInstance(Board.get(i))) n++;
        }
        return n;
    }

    private void requirePendingBuy(TyPhuState s) {
        if (s.pendingBuy == null) {
            throw new GameException("Không có quyết định mua nào đang chờ");
        }
    }

    private Tile tileOrThrow(int index) {
        if (index < 0 || index >= Board.SIZE) {
            throw new GameException("Ô đất không hợp lệ");
        }
        return Board.get(index);
    }
}
