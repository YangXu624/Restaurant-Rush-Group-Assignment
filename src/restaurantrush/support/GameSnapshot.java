package restaurantrush.support;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import restaurantrush.control.TurnManager;
import restaurantrush.entity.Customer;
import restaurantrush.entity.Order;
import restaurantrush.entity.Restaurant;
import restaurantrush.entity.Table;

/**
 * Immutable display state captured from the restaurant and turn manager.
 * Includes every arrived customer, including paid and departed customers, in
 * arrival order. A null paid average means nobody has paid yet.
 *
 * <p>Lists are immutable copies containing only immutable values. Later domain
 * changes cannot alter this snapshot. Capture between game actions on the game
 * thread; this factory does not synchronize concurrent changes.
 *
 * <p>Owner: M5: Toshiki. See docs/class-contracts.md for the contract.
 */
public record GameSnapshot(
        int turn,
        int revenueCents,
        Double averagePaidSatisfaction,
        List<Integer> waitingCustomerIds,
        List<CustomerView> customers,
        List<TableView> tables,
        boolean waiterAvailable,
        boolean chefAvailable,
        int paidCount,
        int servedCount,
        int abandonedCount) {

    /**
     * Copies lists even when the caller constructs the record directly.
     *
     * @throws NullPointerException if a list or any list element is null
     */
    public GameSnapshot {
        waitingCustomerIds = List.copyOf(waitingCustomerIds);
        customers = List.copyOf(customers);
        tables = List.copyOf(tables);
    }

    /**
     * Reads state without performing actions or retaining mutable domain objects.
     * Availability comes from the current phase, not from inferred task usage.
     *
     * @throws IllegalArgumentException if either collaborator is null
     */
    public static GameSnapshot from(Restaurant r, TurnManager t) {
        if (r == null || t == null) {
            throw new IllegalArgumentException("Snapshot collaborators cannot be null");
        }

        List<Integer> waitingIds = new ArrayList<>();
        for (Customer customer : r.getWaitingQueue()) {
            waitingIds.add(customer.getId());
        }

        List<TableView> tableViews = new ArrayList<>();
        Map<Integer, Integer> tableIdsByCustomer = new HashMap<>();
        for (Table table : r.getTables()) {
            Customer occupant = table.getOccupant();
            Integer occupantId = occupant == null ? null : occupant.getId();
            tableViews.add(new TableView(table.getId(), occupantId));
            if (occupantId != null) {
                tableIdsByCustomer.put(occupantId, table.getId());
            }
        }

        List<CustomerView> customerViews = new ArrayList<>();
        for (Customer customer : r.getCustomers()) {
            Order order = customer.getOrder();
            customerViews.add(new CustomerView(
                    customer.getId(), customer.getSatisfaction(), customer.getStatus(),
                    order == null ? null : order.getStatus(),
                    order == null ? null : order.getItem().getName(),
                    order == null ? null : order.getRemainingUnits(),
                    tableIdsByCustomer.get(customer.getId())));
        }

        int paidCount = r.getPaidCount();
        return new GameSnapshot(t.getCurrentTurn(), r.getRevenueCents(),
                paidCount == 0 ? null : r.averagePaidSatisfaction(),
                waitingIds, customerViews, tableViews,
                t.canAssignWaiter(), t.canAssignChef(), paidCount,
                r.getServedCount(), r.getAbandonedCount());
    }
}
