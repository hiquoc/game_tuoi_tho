package com.vietgames.arena.cotyphu;

import com.vietgames.arena.platform.GameAction;

/** All legal player actions. The client only ever sends these; the server
 *  owns everything else (dice, cards, rent math). */
public sealed interface TyPhuAction extends GameAction permits
        TyPhuAction.RollDice,
        TyPhuAction.BuyProperty,
        TyPhuAction.DeclineBuy,
        TyPhuAction.BuildHouse,
        TyPhuAction.EndTurn,
        TyPhuAction.PayJailFine,
        TyPhuAction.UseJailCard {

    record RollDice() implements TyPhuAction {
    }

    record BuyProperty() implements TyPhuAction {
    }

    record DeclineBuy() implements TyPhuAction {
    }

    record BuildHouse(int tileIndex) implements TyPhuAction {
    }

    record EndTurn() implements TyPhuAction {
    }

    record PayJailFine() implements TyPhuAction {
    }

    record UseJailCard() implements TyPhuAction {
    }
}
