package restaurantrush.entity;

import java.util.List;

/**
 * Stage 1 skeleton. Owner: M1: Yang Xu.
 * See docs/class-contracts.md for the implementation contract.
 */
public class Customer {
    private final int id;
    private int satisfaction;
    private CustomerStatus status;
    private int servedTurn;
    private int readyToPayTurn;
    private Order order;

    public Customer(int id) {
        if (id <= 0) {
            throw new IllegalArgumentException("Customer ID must be positive: " + id);
        }
        this.id = id;
        this.satisfaction = 100;
        this.status = CustomerStatus.WAITING;
        this.servedTurn = -1;
        this.readyToPayTurn = -1;
        this.order = null;
    }

    public MenuItem chooseItem(List<MenuItem> menu) {
        if (menu == null || menu.isEmpty()) {
            throw new IllegalArgumentException("Menu cannot be null or empty");
        }
        return menu.get((id - 1) % menu.size());
    }

    public void seat() {
        if (status != CustomerStatus.WAITING) {
            throw new IllegalStateException("Customer " + id + " cannot be seated with status " + status);
        }
        status = CustomerStatus.SEATED;
    }

    public void acceptOrder(Order order) {
        if (order == null) {
            throw new IllegalArgumentException("Order cannot be null");
        }
        if (order.getId() != id) {
            throw new IllegalArgumentException(
                    "Order ID " + order.getId() + " does not match customer ID " + id);
        }
        if (status != CustomerStatus.SEATED) {
            throw new IllegalStateException("Customer " + id + " cannot accept an order with status " + status);
        }
        if (this.order != null) {
            throw new IllegalStateException("Customer " + id + " already has an order");
        }
        this.order = order;
    }

    public void markServed(int turn) {
        throw new UnsupportedOperationException("Customer.markServed is not implemented yet");
    }

    public void finishEating(int turn) {
        throw new UnsupportedOperationException("Customer.finishEating is not implemented yet");
    }

    public void applyWaitingPenalty() {
        throw new UnsupportedOperationException("Customer.applyWaitingPenalty is not implemented yet");
    }

    public void markPaid() {
        throw new UnsupportedOperationException("Customer.markPaid is not implemented yet");
    }

    public void abandon() {
        throw new UnsupportedOperationException("Customer.abandon is not implemented yet");
    }

    public int getId() {
        return id;
    }

    public int getSatisfaction() {
        return satisfaction;
    }

    public CustomerStatus getStatus() {
        return status;
    }

    public Order getOrder() {
        return order;
    }

    public int getServedTurn() {
        return servedTurn;
    }

    public int getReadyToPayTurn() {
        return readyToPayTurn;
    }
}
