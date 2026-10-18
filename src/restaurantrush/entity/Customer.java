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
        if (turn <= 0) {
            throw new IllegalArgumentException("Turn must be positive: " + turn);
        }
        if (status != CustomerStatus.SEATED) {
            throw new IllegalStateException("Customer " + id + " cannot be served in status " + status);
        }
        if (servedTurn != -1) {
            throw new IllegalStateException("Customer " + id + " was already served at turn " + servedTurn);
        }
        if (order == null) {
            throw new IllegalStateException("Customer " + id + " cannot be served without an order");
        }
        if (order.getStatus() != OrderStatus.SERVED) {
            throw new IllegalStateException(
                    "Customer " + id + "'s order is not SERVED, status is " + order.getStatus());
        }
        status = CustomerStatus.SERVED;
        servedTurn = turn;
    }

    public void finishEating(int turn) {
        if (status == CustomerStatus.SERVED && servedTurn != -1 && servedTurn < turn) {
            status = CustomerStatus.READY_TO_PAY;
            readyToPayTurn = turn;
        }
    }

    public void applyWaitingPenalty() {
        if (status == CustomerStatus.WAITING || status == CustomerStatus.SEATED) {
            satisfaction = Math.max(0, satisfaction - 8);
        }
    }

    public void markPaid() {
        if (status != CustomerStatus.READY_TO_PAY) {
            throw new IllegalStateException("Customer " + id + " cannot pay in status " + status);
        }
        if (order == null || order.getStatus() != OrderStatus.PAID) {
            throw new IllegalStateException(
                    "Customer " + id + " cannot be marked paid before its order is marked PAID");
        }
        status = CustomerStatus.PAID;
    }

    public void abandon() {
        if (status != CustomerStatus.WAITING && status != CustomerStatus.SEATED) {
            throw new IllegalStateException("Customer " + id + " cannot abandon in status " + status);
        }
        if (order != null) {
            order.cancel();
        }
        status = CustomerStatus.LEFT;
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
