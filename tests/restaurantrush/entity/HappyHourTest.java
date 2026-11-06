package restaurantrush.entity;

import java.util.Objects;

/** Duration, single activation and discount rules for the promotion on its own. */
public class HappyHourTest {
    public static void main(String[] args) {
        testLastsForActivationTurnAndNext();
        testActivatesOnlyOnce();
        testDiscountInIntegerCents();
        System.out.println("All HappyHour tests passed successfully!");
    }

    private static void testLastsForActivationTurnAndNext() {
        HappyHour happyHour = new HappyHour();
        equal(false, happyHour.isActive(), "Off until the manager activates it");
        happyHour.endTurn();
        equal(0, happyHour.getTurnsRemaining(), "Ending a turn while off changes nothing");

        // Activated on turn 5.
        equal(true, happyHour.activate().isSuccess(), "First activation accepted");
        equal(true, happyHour.isActive(), "Active on turn 5");
        equal(2, happyHour.getTurnsRemaining(), "Turns 5 and 6 remain");
        happyHour.endTurn();
        equal(true, happyHour.isActive(), "Active on turn 6");
        equal(1, happyHour.getTurnsRemaining(), "Turn 6 remains");
        happyHour.endTurn();
        equal(false, happyHour.isActive(), "Expired on turn 7");
        equal(0, happyHour.getTurnsRemaining(), "Nothing remains");
    }

    private static void testActivatesOnlyOnce() {
        HappyHour happyHour = new HappyHour();
        equal(false, happyHour.isUsed(), "Unused at the start");
        happyHour.activate();
        equal(true, happyHour.isUsed(), "Used after activation");
        equal(false, happyHour.activate().isSuccess(), "Second activation fails while active");
        equal(2, happyHour.getTurnsRemaining(), "Failed activation does not extend the duration");
        happyHour.endTurn();
        happyHour.endTurn();
        equal(false, happyHour.activate().isSuccess(), "Cannot activate again after expiry");
        equal(false, happyHour.isActive(), "Failed activation does not restart it");
    }

    private static void testDiscountInIntegerCents() {
        HappyHour happyHour = new HappyHour();
        equal(1440, happyHour.applyDiscount(1800), "Pasta");
        equal(1080, happyHour.applyDiscount(1350), "Discount applies to the VIP's Burger bill");
        equal(1944, happyHour.applyDiscount(2430), "Combo");
        equal(0, happyHour.applyDiscount(0), "Free item");
        equal(2, happyHour.applyDiscount(3), "2.4 cents rounds down");
        equal(6, happyHour.applyDiscount(7), "5.6 cents rounds up");
    }

    private static void equal(Object expected, Object actual, String message) {
        if (!Objects.equals(expected, actual)) {
            throw new AssertionError(message + ": expected " + expected + ", got " + actual);
        }
    }
}
