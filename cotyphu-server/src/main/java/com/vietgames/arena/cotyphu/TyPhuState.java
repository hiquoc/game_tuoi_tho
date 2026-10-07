package com.vietgames.arena.cotyphu;

import com.vietgames.arena.platform.GameState;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

import static com.vietgames.arena.cotyphu.Card.CardKind.*;

/** Authoritative state of one Co Ty Phu game. */
public class TyPhuState implements GameState {

    public enum Phase { IN_PROGRESS, FINISHED }

    public Phase phase = Phase.IN_PROGRESS;
    /** Turn order. */
    public final List<PlayerState> players = new ArrayList<>();
    public int currentPlayerIdx = 0;
    public int dice1 = 0;
    public int dice2 = 0;
    public boolean hasRolled = false;
    public int doublesCount = 0;
    /** Tile index awaiting a buy/decline decision, or null. */
    public Integer pendingBuy = null;
    public String winnerId = null;
    public final Deque<Card> chanceDeck = new ArrayDeque<>();
    public final Deque<Card> communityDeck = new ArrayDeque<>();
    public final List<String> log = new ArrayList<>();

    public PlayerState currentPlayer() {
        return players.get(currentPlayerIdx);
    }

    public void log(String message) {
        log.add(message);
        while (log.size() > 60) {
            log.remove(0);
        }
    }

    /** Builds and shuffles both decks. Called once per game. */
    public void initDecks() {
        List<Card> chance = new ArrayList<>(List.of(
                new Card("Tiến thẳng tới Xuất phát", ADVANCE_GO, 0),
                new Card("Vào tù ngay", GO_TO_JAIL, 0),
                new Card("Lùi 3 ô", BACK_3, 0),
                new Card("Nộp phạt 50", PAY_BANK, 50),
                new Card("Trúng thưởng 100", RECEIVE_BANK, 100),
                new Card("Thẻ ra tù miễn phí", JAIL_FREE, 0),
                new Card("Mỗi người chơi tặng bạn 50", GIFT_FROM_ALL, 50),
                new Card("Nộp thuế 100", PAY_BANK, 100)));
        List<Card> community = new ArrayList<>(List.of(
                new Card("Tiến thẳng tới Xuất phát", ADVANCE_GO, 0),
                new Card("Vào tù ngay", GO_TO_JAIL, 0),
                new Card("Được tặng 200", RECEIVE_BANK, 200),
                new Card("Nộp phạt 100", PAY_BANK, 100),
                new Card("Thẻ ra tù miễn phí", JAIL_FREE, 0),
                new Card("Lì xì 50", RECEIVE_BANK, 50),
                new Card("Mỗi người chơi tặng bạn 50", GIFT_FROM_ALL, 50),
                new Card("Sinh nhật! Nhận 100", RECEIVE_BANK, 100)));
        java.util.Collections.shuffle(chance);
        java.util.Collections.shuffle(community);
        chanceDeck.addAll(chance);
        communityDeck.addAll(community);
    }
}
