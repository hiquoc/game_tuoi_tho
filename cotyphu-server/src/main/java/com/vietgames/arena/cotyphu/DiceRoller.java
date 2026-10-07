package com.vietgames.arena.cotyphu;

import java.util.Random;

/** Dice roller abstraction so tests can fix the dice. */
public interface DiceRoller {
    /** Returns a value 1-6. */
    int roll();

    class RandomDiceRoller implements DiceRoller {
        private final Random random = new Random();

        @Override
        public int roll() {
            return random.nextInt(6) + 1;
        }
    }

    /** Cycles through the given values — for deterministic tests. */
    class FixedDiceRoller implements DiceRoller {
        private final int[] values;
        private int cursor = 0;

        public FixedDiceRoller(int... values) {
            if (values.length == 0) {
                throw new IllegalArgumentException("Need at least one die value");
            }
            this.values = values;
        }

        @Override
        public int roll() {
            int v = values[cursor % values.length];
            cursor++;
            return v;
        }
    }
}
