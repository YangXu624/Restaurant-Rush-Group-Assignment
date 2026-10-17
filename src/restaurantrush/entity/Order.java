package restaurantrush.entity;

/**
 * Stage 1 skeleton. Owner: M1: Yang Xu.
 * See docs/class-contracts.md for the implementation contract.
 */
public class Order {
    private int id;
    private MenuItem item;
    private OrderStatus status;
    private int remainingUnits;
    private int billCents;

    public Order(int id, MenuItem item) {
        throw new UnsupportedOperationException("Order.Order is not implemented yet");
    }

    public boolean canPrepare() {
        throw new UnsupportedOperationException("Order.canPrepare is not implemented yet");
    }

    public void prepareOneUnit() {
        throw new UnsupportedOperationException("Order.prepareOneUnit is not implemented yet");
    }

    public void markServed() {
        throw new UnsupportedOperationException("Order.markServed is not implemented yet");
    }

    public void markPaid() {
        throw new UnsupportedOperationException("Order.markPaid is not implemented yet");
    }

    public void cancel() {
        throw new UnsupportedOperationException("Order.cancel is not implemented yet");
    }

    public boolean isReady() {
        throw new UnsupportedOperationException("Order.isReady is not implemented yet");
    }

    public int getBillCents() {
        throw new UnsupportedOperationException("Order.getBillCents is not implemented yet");
    }

    public int getId() {
        throw new UnsupportedOperationException("Order.getId is not implemented yet");
    }

    public OrderStatus getStatus() {
        throw new UnsupportedOperationException("Order.getStatus is not implemented yet");
    }

    public MenuItem getItem() {
        throw new UnsupportedOperationException("Order.getItem is not implemented yet");
    }

    public int getRemainingUnits() {
        throw new UnsupportedOperationException("Order.getRemainingUnits is not implemented yet");
    }
}
