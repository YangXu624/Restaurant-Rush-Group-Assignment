package restaurantrush.entity;

import restaurantrush.support.ActionResult;

/** Brief 4.3: a promotion the manager may switch on once, for two turns. */
public class HappyHour {
    private static final int DURATION_TURNS = 2;
    private static final int DISCOUNTED_PERCENT = 80;

    private boolean used = false;
    private int turnsRemaining = 0;

    /** A second activation fails and changes nothing. */
    public ActionResult activate() {
        if (used) {
            return ActionResult.fail("Happy Hour can only be activated once per game.");
        }
        used = true;
        turnsRemaining = DURATION_TURNS;
        return ActionResult.ok("Happy Hour is active for " + DURATION_TURNS + " turns.");
    }

    /** Called once when a turn ends, so the activation turn counts as the first turn. */
    public void endTurn() {
        if (turnsRemaining > 0) {
            turnsRemaining--;
        }
    }

    /** Returns the bill after 20% off, rounded to the nearest cent (half up). */
    public int applyDiscount(int billCents) {
        if (billCents < 0) {
            throw new IllegalArgumentException("Bill cannot be negative");
        }
        long discounted = (long) billCents * DISCOUNTED_PERCENT;
        return Math.toIntExact((discounted + 50) / 100);
    }

    public boolean isActive() {
        return turnsRemaining > 0;
    }

    public boolean isUsed() {
        return used;
    }

    public int getTurnsRemaining() {
        return turnsRemaining;
    }
}
