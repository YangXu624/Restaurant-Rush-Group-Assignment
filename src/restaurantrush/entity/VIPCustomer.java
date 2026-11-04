package restaurantrush.entity;

/** VIP behavior for Stage 2; lifecycle and state remain in Customer. */
public class VIPCustomer extends Customer {
    public VIPCustomer(int id) {
        super(id);
    }

    @Override
    protected int waitingPenaltyPerTurn() {
        return 5;
    }

    /** Applies 10% off, rounding half up to the nearest cent using integer arithmetic. */
    @Override
    public int calculateBillCents(MenuItem item) {
        int priceCents = super.calculateBillCents(item);
        return Math.toIntExact((priceCents * 90L + 50L) / 100L);
    }

    @Override
    public String getCustomerType() {
        return "VIP";
    }
}
