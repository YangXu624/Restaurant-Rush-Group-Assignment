package restaurantrush.entity;

import java.util.List;
import restaurantrush.support.ActionResult;

/**
 * Stage 1 skeleton. Owner: M2: Kai Xuan.
 * See docs/class-contracts.md for the implementation contract.
 */
public class Restaurant {
    private List<Customer> waitingQueue;
    private List<Customer> customers;
    private List<Table> tables;
    private List<MenuItem> menu;
    private List<Order> orders;
    private int revenueCents;
    private int servedCount;
    private int abandonedCount;
    private int paidSatisfactionTotal;
    private int paidCount;

    public Restaurant() {
        throw new UnsupportedOperationException("Restaurant.Restaurant is not implemented yet");
    }

    public void addArrival(Customer c) {
        throw new UnsupportedOperationException("Restaurant.addArrival is not implemented yet");
    }

    public ActionResult seat(int cId, int tableId) {
        throw new UnsupportedOperationException("Restaurant.seat is not implemented yet");
    }

    public ActionResult serve(int customerId, int turn) {
        throw new UnsupportedOperationException("Restaurant.serve is not implemented yet");
    }

    public Customer findCustomer(int id) {
        throw new UnsupportedOperationException("Restaurant.findCustomer is not implemented yet");
    }

    public Order findOrder(int id) {
        throw new UnsupportedOperationException("Restaurant.findOrder is not implemented yet");
    }

    public void registerOrder(Order order) {
        throw new UnsupportedOperationException("Restaurant.registerOrder is not implemented yet");
    }

    public boolean recordPayment(Customer c, int turn) {
        throw new UnsupportedOperationException("Restaurant.recordPayment is not implemented yet");
    }

    public void removeAbandoned(Customer c) {
        throw new UnsupportedOperationException("Restaurant.removeAbandoned is not implemented yet");
    }

    public Customer oldestEligiblePayer(int turn) {
        throw new UnsupportedOperationException("Restaurant.oldestEligiblePayer is not implemented yet");
    }

    public double averagePaidSatisfaction() {
        throw new UnsupportedOperationException("Restaurant.averagePaidSatisfaction is not implemented yet");
    }

    public List<Customer> getActiveCustomers() {
        throw new UnsupportedOperationException("Restaurant.getActiveCustomers is not implemented yet");
    }

    public List<Customer> getWaitingQueue() {
        throw new UnsupportedOperationException("Restaurant.getWaitingQueue is not implemented yet");
    }

    public List<Customer> getCustomers() {
        throw new UnsupportedOperationException("Restaurant.getCustomers is not implemented yet");
    }

    public List<Table> getTables() {
        throw new UnsupportedOperationException("Restaurant.getTables is not implemented yet");
    }

    public List<MenuItem> getMenu() {
        throw new UnsupportedOperationException("Restaurant.getMenu is not implemented yet");
    }

    public List<Order> getOrders() {
        throw new UnsupportedOperationException("Restaurant.getOrders is not implemented yet");
    }

    public int getRevenueCents() {
        throw new UnsupportedOperationException("Restaurant.getRevenueCents is not implemented yet");
    }

    public int getPaidCount() {
        throw new UnsupportedOperationException("Restaurant.getPaidCount is not implemented yet");
    }

    public int getServedCount() {
        throw new UnsupportedOperationException("Restaurant.getServedCount is not implemented yet");
    }

    public int getAbandonedCount() {
        throw new UnsupportedOperationException("Restaurant.getAbandonedCount is not implemented yet");
    }

    public int getPaidSatisfactionTotal() {
        throw new UnsupportedOperationException("Restaurant.getPaidSatisfactionTotal is not implemented yet");
    }
}
