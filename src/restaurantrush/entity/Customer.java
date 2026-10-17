package restaurantrush.entity;

import java.util.List;

/**
 * Stage 1 skeleton. Owner: M1: Yang Xu.
 * See docs/class-contracts.md for the implementation contract.
 */
public class Customer {
    private int id;
    private int satisfaction;
    private CustomerStatus status;
    private int servedTurn;
    private int readyToPayTurn;
    private Order order;

    public Customer(int id) {
        throw new UnsupportedOperationException("Customer.Customer is not implemented yet");
    }

    public MenuItem chooseItem(List<MenuItem> menu) {
        throw new UnsupportedOperationException("Customer.chooseItem is not implemented yet");
    }

    public void seat() {
        throw new UnsupportedOperationException("Customer.seat is not implemented yet");
    }

    public void acceptOrder(Order order) {
        throw new UnsupportedOperationException("Customer.acceptOrder is not implemented yet");
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
        throw new UnsupportedOperationException("Customer.getId is not implemented yet");
    }

    public int getSatisfaction() {
        throw new UnsupportedOperationException("Customer.getSatisfaction is not implemented yet");
    }

    public CustomerStatus getStatus() {
        throw new UnsupportedOperationException("Customer.getStatus is not implemented yet");
    }

    public Order getOrder() {
        throw new UnsupportedOperationException("Customer.getOrder is not implemented yet");
    }

    public int getServedTurn() {
        throw new UnsupportedOperationException("Customer.getServedTurn is not implemented yet");
    }

    public int getReadyToPayTurn() {
        throw new UnsupportedOperationException("Customer.getReadyToPayTurn is not implemented yet");
    }
}
