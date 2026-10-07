package com.vietgames.arena.cotyphu;

import com.vietgames.arena.platform.GameException;
import com.vietgames.arena.platform.PlayerInfo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Deterministic tests using fixed dice. The engine is a pure state machine,
 * so every rule is verifiable without Spring or sockets.
 */
class TyPhuEngineTest {

    private TyPhuEngine engine;
    private TyPhuState state;

    @BeforeEach
    void setUp() {
        // dice sequence: (1,2) (1,2) (1,2) ... lands on tile 3 from GO
        engine = new TyPhuEngine(new DiceRoller.FixedDiceRoller(1, 2));
        state = (TyPhuState) engine.newGame(List.of(
                new PlayerInfo("p1", "An"),
                new PlayerInfo("p2", "Bình")));
    }

    private PlayerState p1() { return state.players.get(0); }
    private PlayerState p2() { return state.players.get(1); }

    private void act(String playerId, TyPhuAction action) {
        engine.applyAction(state, playerId, action);
    }

    @Test
    void buyPropertyDeductsMoneyAndAssignsOwner() {
        act("p1", new TyPhuAction.RollDice()); // 1+2=3 -> Phố Bạch Mai (60)
        assertEquals(3, p1().position);
        assertEquals(3, state.pendingBuy);

        act("p1", new TyPhuAction.BuyProperty());
        assertEquals(1500 - 60, p1().money);
        assertTrue(p1().properties.contains(3));
        assertNull(state.pendingBuy);
    }

    @Test
    void rentIsPaidToOwner() {
        act("p1", new TyPhuAction.RollDice());
        act("p1", new TyPhuAction.BuyProperty());
        act("p1", new TyPhuAction.EndTurn());

        int p1Before = p1().money;
        act("p2", new TyPhuAction.RollDice()); // lands on tile 3, owned by p1
        // base rent = 60/30 = 2
        assertEquals(1500 - 2, p2().money);
        assertEquals(p1Before + 2, p1().money);
    }

    @Test
    void fullColorSetDoublesBaseRent() {
        // p1 buys both brown properties (tiles 1 and 3)
        p1().position = 0;
        act("p1", new TyPhuAction.RollDice()); // -> 3
        act("p1", new TyPhuAction.BuyProperty());
        act("p1", new TyPhuAction.EndTurn());
        act("p2", new TyPhuAction.EndTurn()); // p2 passes (already rolled? no -> must roll first)
        // p2 hasn't rolled; endTurn by p2 is illegal (not rolled, but endTurn is always allowed)
        // turn is back to p1; move p1 to tile 1 via direct setup instead
        p1().properties.add(1);

        act("p1", new TyPhuAction.EndTurn()); // -> p2's turn
        int p1Before = p1().money;
        // force p2 onto tile 3: position 0 + fixed dice (1,2) = 3
        act("p2", new TyPhuAction.RollDice());
        assertEquals(1500 - 4, p2().money); // doubled base rent
        assertEquals(p1Before + 4, p1().money);
    }

    @Test
    void threeDoublesInARowSendsPlayerToJail() {
        engine = new TyPhuEngine(new DiceRoller.FixedDiceRoller(2, 2, 3, 3, 4, 4));
        state = (TyPhuState) engine.newGame(List.of(
                new PlayerInfo("p1", "An"), new PlayerInfo("p2", "Bình")));

        act("p1", new TyPhuAction.RollDice()); // doubles -> roll again
        assertFalse(state.hasRolled);
        act("p1", new TyPhuAction.RollDice()); // doubles -> roll again
        assertFalse(state.hasRolled);
        act("p1", new TyPhuAction.RollDice()); // third doubles -> jail
        assertTrue(p1().inJail);
        assertEquals(Board.JAIL_INDEX, p1().position);
        assertTrue(state.hasRolled);
    }

    @Test
    void cannotAffordTaxMeansBankruptcyAndOpponentWins() {
        p2().money = 50;
        p2().position = 1; // fixed dice (1,2)=3 -> tile 4: income tax 200
        act("p1", new TyPhuAction.EndTurn());
        act("p2", new TyPhuAction.RollDice());
        assertTrue(p2().bankrupt);
        assertEquals(TyPhuState.Phase.FINISHED, state.phase);
        assertEquals("p1", state.winnerId);
    }

    @Test
    void outOfTurnActionIsRejected() {
        assertThrows(GameException.class,
                () -> act("p2", new TyPhuAction.RollDice()));
    }

    @Test
    void mustDecideBuyBeforeEndingTurn() {
        act("p1", new TyPhuAction.RollDice()); // pendingBuy = 3
        assertThrows(GameException.class,
                () -> act("p1", new TyPhuAction.EndTurn()));
        act("p1", new TyPhuAction.DeclineBuy());
        act("p1", new TyPhuAction.EndTurn()); // now fine
        assertEquals("p2", state.currentPlayer().id);
    }

    @Test
    void passingGoCollectsSalary() {
        p1().position = 38;
        act("p1", new TyPhuAction.RollDice()); // 1+2=3 -> wraps to tile 1 (Phố Huế)
        assertEquals(1, p1().position);
        assertEquals(1500 + 200, p1().money); // salary; tile 1 unowned -> pendingBuy, no purchase yet
        assertEquals(1, state.pendingBuy);
    }

    @Test
    void buildHouseRequiresFullSetAndDeductsCost() {
        p1().properties.add(1);
        p1().properties.add(3);
        int before = p1().money;
        engine.applyAction(state, "p1", new TyPhuAction.BuildHouse(3));
        assertEquals(1, p1().housesOn(3));
        assertEquals(before - 50, p1().money); // brown house cost
    }

    @Test
    void buildHouseWithoutFullSetIsRejected() {
        p1().properties.add(3);
        assertThrows(GameException.class,
                () -> engine.applyAction(state, "p1", new TyPhuAction.BuildHouse(3)));
    }

    @Test
    void jailFineReleasesPlayer() {
        p1().inJail = true;
        p1().position = Board.JAIL_INDEX;
        int before = p1().money;
        act("p1", new TyPhuAction.PayJailFine());
        assertFalse(p1().inJail);
        assertEquals(before - TyPhuEngine.JAIL_FINE, p1().money);
    }
}
