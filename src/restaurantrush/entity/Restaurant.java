package restaurantrush.entity;

import java.util.ArrayList;
import java.util.List;
import restaurantrush.support.ActionResult;

/**
 * Restaurant-wide state: the waiting queue, tables, customers, orders and totals.
 *
 * <p>Seating, serving, payment and abandonment each change several objects, so they
 * go through here. Every such operation validates first and only then mutates, so a
 * failed request leaves the game state unchanged.
 *
 * <p>Owner: M2: Kai Xuan. See docs/class-contracts.md for the implementation contract.
 */
public class Restaurant {
    // Brief 2.1: the base setting has 2 tables.
    private static final int TABLE_COUNT = 2;

    private final List<Customer> waitingQueue = new ArrayList<>();
    private final List<Customer> customers = new ArrayList<>();
    private final List<Table> tables = new ArrayList<>();
    private final List<MenuItem> menu = new ArrayList<>();
    private final List<Order> orders = new ArrayList<>();

    // Brief 2.2: money is stored as integer cents.
    private int revenueCents;
    private int servedCount;
    private int abandonedCount;
    private int paidSatisfactionTotal;
    private int paidCount;

    /** Creates two free tables, the base menu, and empty lists and totals. */
    public Restaurant() {
        for (int id = 1; id <= TABLE_COUNT; id++) {
            tables.add(new Table(id));
        }
        // Brief 2.1: base menu with prices in cents and preparation units.
        // Customer.chooseItem() depends on this order.
        menu.add(new MenuItem("Salad", 900, 1));
        menu.add(new MenuItem("Burger", 1500, 1));
        menu.add(new MenuItem("Pasta", 1800, 2));
    }

    /**
     * Adds a newly arrived customer to the history and the back of the queue.
     *
     * @throws IllegalArgumentException if the customer is null, not WAITING,
     *         or has the same ID as an earlier arrival
     */
    public void addArrival(Customer c) {
        if (c == null) {
            throw new IllegalArgumentException("Arrival cannot be null");
        }
        if (c.getStatus() != CustomerStatus.WAITING) {
            throw new IllegalArgumentException(
                    "New arrival " + c.getId() + " must be WAITING, not " + c.getStatus());
        }
        if (findCustomer(c.getId()) != null) {
            throw new IllegalArgumentException("Customer " + c.getId() + " has already arrived");
        }
        customers.add(c);
        waitingQueue.add(c);
    }

    /** Moves a queued customer to a free table. Changes nothing on failure. */
    public ActionResult seat(int cId, int tableId) {
        // Brief 3.4: an invalid choice explains why it failed and changes nothing,
        // so the manager can choose again without using up the Waiter's task.
        Customer c = findCustomer(cId);
        if (c == null) {
            return ActionResult.fail("Customer " + cId + " has not arrived.");
        }
        if (!waitingQueue.contains(c)) {
            return ActionResult.fail("Customer " + cId + " is not waiting for a table.");
        }
        Table table = findTable(tableId);
        if (table == null) {
            return ActionResult.fail("Table " + tableId + " does not exist.");
        }
        if (!table.isAvailable()) {
            return ActionResult.fail("Table " + tableId + " is occupied by customer "
                    + table.getOccupant().getId() + ".");
        }

        waitingQueue.remove(c);
        table.assign(c);
        c.seat();
        return ActionResult.ok("Seated customer " + cId + " at table " + tableId + ".");
    }

    /**
     * Serves a seated customer's READY order. Changes nothing on failure.
     * The Waiter-before-Chef timing is enforced by GameController and TurnManager.
     */
    public ActionResult serve(int customerId, int turn) {
        Customer c = findCustomer(customerId);
        if (c == null) {
            return ActionResult.fail("Customer " + customerId + " has not arrived.");
        }
        if (c.getStatus() != CustomerStatus.SEATED || c.getServedTurn() != -1) {
            return ActionResult.fail("Customer " + customerId
                    + " is not a seated customer waiting for food (" + c.getStatus() + ").");
        }
        if (findTableOf(c) == null) {
            return ActionResult.fail("Customer " + customerId + " is not at a table.");
        }
        Order order = c.getOrder();
        if (order == null) {
            return ActionResult.fail("Customer " + customerId + " has not ordered yet.");
        }
        if (findOrder(customerId) != order) {
            return ActionResult.fail("Customer " + customerId + "'s order is not registered.");
        }
        // Brief 3.2 step 3: a dish can be served only if it is already READY.
        if (!order.isReady()) {
            return ActionResult.fail("Customer " + customerId + "'s " + order.getItem().getName()
                    + " is not ready (" + order.getStatus() + ").");
        }

        // Brief 3.2 step 3: serving sets the order to SERVED.
        order.markServed();
        c.markServed(turn);
        servedCount++; // Brief 3.5: served count
        return ActionResult.ok("Served " + order.getItem().getName()
                + " to customer " + customerId + ".");
    }

    /** Returns the arrived customer with this ID (queued, seated, paid or departed), or null. */
    public Customer findCustomer(int id) {
        for (Customer c : customers) {
            if (c.getId() == id) {
                return c;
            }
        }
        return null;
    }

    /** Returns the order with this ID (same as its customer's ID), or null if none was placed. */
    public Order findOrder(int id) {
        for (Order o : orders) {
            if (o.getId() == id) {
                return o;
            }
        }
        return null;
    }

    /**
     * Records an order after the Waiter has linked it to its customer with
     * Customer.acceptOrder(), so the customer and the registry share one instance.
     *
     * @throws IllegalArgumentException if the order is null or its ID is already registered
     * @throws IllegalStateException if the matching customer has not accepted this order
     */
    public void registerOrder(Order order) {
        if (order == null) {
            throw new IllegalArgumentException("Order cannot be null");
        }
        if (findOrder(order.getId()) != null) {
            throw new IllegalArgumentException("Order " + order.getId() + " is already registered");
        }
        Customer c = findCustomer(order.getId());
        if (c == null || c.getOrder() != order) {
            throw new IllegalStateException(
                    "Order " + order.getId() + " must be accepted by its customer before registering");
        }
        orders.add(order);
    }

    /**
     * Completes one payment: marks the order and customer PAID, frees the table and
     * updates the totals. Returns false and changes nothing if any check fails.
     */
    public boolean recordPayment(Customer c, int turn) {
        if (c == null || findCustomer(c.getId()) != c) {
            return false;
        }
        // Brief 3.4: the Cashier cannot charge before READY_TO_PAY.
        // Brief 3.2 step 4: a customer who became READY_TO_PAY this turn pays next turn at the earliest.
        if (c.getStatus() != CustomerStatus.READY_TO_PAY || c.getReadyToPayTurn() >= turn) {
            return false;
        }
        Order order = c.getOrder();
        if (order == null || order.getStatus() != OrderStatus.SERVED) {
            return false;
        }
        Table table = findTableOf(c);
        if (table == null) {
            return false;
        }

        // Brief 3.2 step 4: payment marks the customer and order PAID, frees the table
        // and adds the bill to revenue.
        order.markPaid();
        c.markPaid();
        table.release();
        revenueCents += order.getBillCents(); // Brief 3.4: revenue only includes paid bills
        // Brief 3.5: the average uses the satisfaction recorded at payment.
        paidSatisfactionTotal += c.getSatisfaction();
        paidCount++;
        return true;
    }

    /**
     * Removes an unserved customer whose satisfaction reached 0: takes them out of the
     * queue or frees their table, then lets the customer leave and cancel their order.
     * Their customer and order records stay in the history.
     *
     * @throws IllegalArgumentException if the customer is null or never arrived here
     * @throws IllegalStateException if the customer is not WAITING/SEATED at 0 satisfaction,
     *         which also stops a departure from being counted twice
     */
    public void removeAbandoned(Customer c) {
        if (c == null || findCustomer(c.getId()) != c) {
            throw new IllegalArgumentException("Unknown customer cannot abandon");
        }
        CustomerStatus status = c.getStatus();
        if (status != CustomerStatus.WAITING && status != CustomerStatus.SEATED) {
            throw new IllegalStateException(
                    "Only unserved customers can abandon; customer " + c.getId() + " is " + status);
        }
        if (c.getSatisfaction() != 0) {
            throw new IllegalStateException(
                    "Customer " + c.getId() + " still has satisfaction " + c.getSatisfaction());
        }
        Table table = findTableOf(c);
        if (!waitingQueue.contains(c) && table == null) {
            throw new IllegalStateException(
                    "Customer " + c.getId() + " is neither queued nor at a table");
        }

        // Brief 3.2 step 5: a customer who reaches 0 leaves without paying; free the
        // table and cancel any unfinished order (done in Customer.abandon()).
        if (!waitingQueue.remove(c)) {
            table.release();
        }
        c.abandon();
        abandonedCount++; // Brief 3.5: unhappy departures
    }

    /**
     * Returns the READY_TO_PAY customer who became ready earliest, strictly before
     * {@code turn}, breaking ties by lower ID. Returns null if nobody is eligible.
     * Only selects the payer; does not take payment.
     */
    public Customer oldestEligiblePayer(int turn) {
        // Brief 3.2 step 4: the Cashier handles the oldest READY_TO_PAY customer first.
        Customer oldest = null;
        for (Customer c : customers) {
            if (c.getStatus() != CustomerStatus.READY_TO_PAY || c.getReadyToPayTurn() >= turn) {
                continue;
            }
            if (oldest == null
                    || c.getReadyToPayTurn() < oldest.getReadyToPayTurn()
                    || (c.getReadyToPayTurn() == oldest.getReadyToPayTurn()
                            && c.getId() < oldest.getId())) {
                oldest = c;
            }
        }
        return oldest;
    }

    /**
     * Returns the average satisfaction recorded at payment, or 0.0 if nobody has paid.
     * GameSnapshot shows N/A in that case.
     */
    public double averagePaidSatisfaction() {
        // Brief 3.5: if no one has paid yet, the display shows N/A.
        if (paidCount == 0) {
            return 0.0;
        }
        return (double) paidSatisfactionTotal / paidCount;
    }

    /** Returns a read-only list of customers still in the restaurant, in arrival order. */
    public List<Customer> getActiveCustomers() {
        List<Customer> active = new ArrayList<>();
        for (Customer c : customers) {
            CustomerStatus status = c.getStatus();
            if (status == CustomerStatus.WAITING || status == CustomerStatus.SEATED
                    || status == CustomerStatus.SERVED || status == CustomerStatus.READY_TO_PAY) {
                active.add(c);
            }
        }
        return List.copyOf(active);
    }

    /** Returns a read-only copy of the queue, in arrival order. */
    public List<Customer> getWaitingQueue() {
        return List.copyOf(waitingQueue);
    }

    /** Returns a read-only copy of every arrived customer, including paid and departed ones. */
    public List<Customer> getCustomers() {
        return List.copyOf(customers);
    }

    /** Returns a read-only copy of the tables, in ID order. */
    public List<Table> getTables() {
        return List.copyOf(tables);
    }

    /** Returns a read-only copy of the menu in Salad, Burger, Pasta order. */
    public List<MenuItem> getMenu() {
        return List.copyOf(menu);
    }

    /** Returns a read-only copy of every order, including paid and cancelled ones. */
    public List<Order> getOrders() {
        return List.copyOf(orders);
    }

    public int getRevenueCents() {
        return revenueCents;
    }

    public int getPaidCount() {
        return paidCount;
    }

    public int getServedCount() {
        return servedCount;
    }

    public int getAbandonedCount() {
        return abandonedCount;
    }

    public int getPaidSatisfactionTotal() {
        return paidSatisfactionTotal;
    }

    private Table findTable(int tableId) {
        for (Table t : tables) {
            if (t.getId() == tableId) {
                return t;
            }
        }
        return null;
    }

    private Table findTableOf(Customer c) {
        for (Table t : tables) {
            if (t.getOccupant() == c) {
                return t;
            }
        }
        return null;
    }
}
