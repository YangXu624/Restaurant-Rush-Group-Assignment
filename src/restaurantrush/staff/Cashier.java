package restaurantrush.staff;

import restaurantrush.entity.Customer;
import restaurantrush.entity.Restaurant;

/**
 * Collects at most one payment per turn. Automatic: TurnManager calls it during
 * turn resolution rather than the manager choosing it.
 *
 * <p>Owner: M3: Marcus. See docs/class-contracts.md for the implementation contract.
 */
public class Cashier {
    private int lastPaidTurn = -1;

    /**
     * Takes payment from the oldest eligible READY_TO_PAY customer. Returns false if
     * a payment was already taken this turn, nobody is eligible or the payment fails.
     */
    public boolean collectOne(Restaurant r, int turn) {
        // Brief 3.2 step 4: the Cashier handles one customer per turn.
        if (lastPaidTurn == turn) {
            return false;
        }
        // Restaurant only returns customers who became ready before this turn.
        Customer payer = r.oldestEligiblePayer(turn);
        if (payer == null) {
            return false;
        }
        if (!r.recordPayment(payer, turn)) {
            return false;
        }
        lastPaidTurn = turn;
        return true;
    }
}
