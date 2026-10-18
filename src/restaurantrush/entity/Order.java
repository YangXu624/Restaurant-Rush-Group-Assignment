package restaurantrush.entity;

/**
 * Stage 1 skeleton. Owner: M1: Yang Xu.
 * See docs/class-contracts.md for the implementation contract.
 */
public class Order {
    private final int id;
    private final MenuItem item;
    private OrderStatus status;
    private int remainingUnits;
    private final int billCents;

    public Order(int id, MenuItem item) {
        if (id <= 0) {
            throw new IllegalArgumentException("Order ID must be positive: " + id);
        }
        if (item == null) {
            throw new IllegalArgumentException("Order item cannot be null");
        }
        if (item.getPreparationUnits() <= 0) {
            throw new IllegalArgumentException("Order item preparation units must be positive");
        }
        this.id = id;
        this.item = item;
        this.status = OrderStatus.PLACED;
        this.remainingUnits = item.getPreparationUnits();
        this.billCents = item.getPriceCents();
    }

    public boolean canPrepare() {
        return (status == OrderStatus.PLACED || status == OrderStatus.PREPARING) && remainingUnits > 0;
    }

    public void prepareOneUnit() {
        if (!canPrepare()) {
            throw new IllegalStateException(
                    "Order " + id + " cannot be prepared in status " + status + " with " + remainingUnits + " units left");
        }
        remainingUnits--;
        if (remainingUnits == 0) {
            status = OrderStatus.READY;
        } else {
            status = OrderStatus.PREPARING;
        }
    }

    public void markServed() {
        if (status != OrderStatus.READY) {
            throw new IllegalStateException("Cannot serve order " + id + " with status " + status);
        }
        status = OrderStatus.SERVED;
    }

    public void markPaid() {
        if (status != OrderStatus.SERVED) {
            throw new IllegalStateException("Cannot mark order " + id + " paid with status " + status);
        }
        status = OrderStatus.PAID;
    }

    public void cancel() {
        if (status != OrderStatus.PLACED && status != OrderStatus.PREPARING && status != OrderStatus.READY) {
            throw new IllegalStateException("Cannot cancel order " + id + " with status " + status);
        }
        status = OrderStatus.CANCELLED;
    }

    public boolean isReady() {
        return status == OrderStatus.READY;
    }

    public int getBillCents() {
        return billCents;
    }

    public int getId() {
        return id;
    }

    public OrderStatus getStatus() {
        return status;
    }

    public MenuItem getItem() {
        return item;
    }

    public int getRemainingUnits() {
        return remainingUnits;
    }
}
