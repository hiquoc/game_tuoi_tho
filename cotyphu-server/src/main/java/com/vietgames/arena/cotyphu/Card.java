package com.vietgames.arena.cotyphu;

/** Chance ("Cơ hội") and Community Chest ("Khí vận") cards. */
public record Card(String text, CardKind kind, int value) {

    public enum CardKind {
        /** Move to GO and collect salary. */
        ADVANCE_GO,
        /** Go directly to jail. */
        GO_TO_JAIL,
        /** Move back 3 tiles and resolve. */
        BACK_3,
        /** Pay the bank. */
        PAY_BANK,
        /** Receive from the bank. */
        RECEIVE_BANK,
        /** Keep a get-out-of-jail-free card. */
        JAIL_FREE,
        /** Every other active player pays you `value`. */
        GIFT_FROM_ALL
    }
}
