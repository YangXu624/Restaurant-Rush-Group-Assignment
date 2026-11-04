package restaurantrush.entity;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.function.IntFunction;

/** Exercises customer behavior through Customer references, without a test framework. */
public class CustomerTypesTest {
    public static void main(String[] args) {
        testConstructionAndLabels();
        testWaitingPenaltiesAndClamping();
        testVipBilling();
        testFoodPreferences();
        testInvalidInputs();
        testInheritedLifecycle();
        testInheritedAbandonment();
        System.out.println("All CustomerTypes tests passed successfully!");
    }

    private static List<Customer> customers() {
        return List.of(new Customer(5), new VIPCustomer(5), new CriticCustomer(5));
    }

    private static List<MenuItem> baseMenu() {
        return List.of(new MenuItem("Salad", 900, 1), new MenuItem("Burger", 1500, 1),
                new MenuItem("Pasta", 1800, 2));
    }

    private static void testConstructionAndLabels() {
        List<Customer> customers = customers();
        List<String> labels = List.of("Regular", "VIP", "Critic");
        for (int i = 0; i < customers.size(); i++) {
            Customer customer = customers.get(i);
            equal(labels.get(i), customer.getCustomerType(), "Polymorphic display label");
            equal(5, customer.getId(), "Inherited identity");
            equal(100, customer.getSatisfaction(), "Initial satisfaction");
            equal(CustomerStatus.WAITING, customer.getStatus(), "Initial status");
            equal(null, customer.getOrder(), "No initial order");
            equal(-1, customer.getServedTurn(), "No service timestamp");
            equal(-1, customer.getReadyToPayTurn(), "No payment-ready timestamp");
        }
        List<IntFunction<Customer>> constructors = List.of(Customer::new, VIPCustomer::new, CriticCustomer::new);
        for (IntFunction<Customer> constructor : constructors) {
            throwsType(IllegalArgumentException.class, () -> constructor.apply(0), "Zero customer ID");
            throwsType(IllegalArgumentException.class, () -> constructor.apply(-1), "Negative customer ID");
            equal(7, constructor.apply(7).getId(), "Types are not restricted to scheduled arrival IDs");
        }
    }

    private static void testWaitingPenaltiesAndClamping() {
        int[] afterOnePenalty = {92, 95, 88};
        int[] turnsBeforeZero = {12, 19, 8};
        int[] justBeforeZero = {4, 5, 4};
        for (boolean seated : new boolean[] {false, true}) {
            List<Customer> customers = customers();
            for (int i = 0; i < customers.size(); i++) {
                Customer customer = customers.get(i);
                if (seated) {
                    customer.seat();
                }
                customer.applyWaitingPenalty();
                equal(afterOnePenalty[i], customer.getSatisfaction(), "Customer-specific waiting penalty");
                for (int turn = 1; turn < turnsBeforeZero[i]; turn++) {
                    customer.applyWaitingPenalty();
                }
                equal(justBeforeZero[i], customer.getSatisfaction(), "Satisfaction before reaching zero");
                customer.applyWaitingPenalty();
                equal(0, customer.getSatisfaction(), "Penalty clamps to zero");
                customer.applyWaitingPenalty();
                equal(0, customer.getSatisfaction(), "Repeated penalties cannot make satisfaction negative");
                equal(seated ? CustomerStatus.SEATED : CustomerStatus.WAITING, customer.getStatus(),
                        "Penalty does not perform abandonment");
            }
        }
    }

    private static void testVipBilling() {
        Customer vip = new VIPCustomer(2);
        int[][] cases = {{900, 810}, {1500, 1350}, {1800, 1620}, {0, 0}, {1, 1},
                {104, 94}, {105, 95}, {106, 95}, {Integer.MAX_VALUE, 1932735282}};
        for (int[] example : cases) {
            MenuItem item = new MenuItem("Special", example[0], 1);
            equal(example[1], vip.calculateBillCents(item), "VIP discount with half-up rounding");
            equal(example[1], vip.calculateBillCents(item), "Repeated quotes do not compound discounts");
            equal(example[0], item.getPriceCents(), "Discount does not mutate the menu price");
            equal(example[0], new Customer(2).calculateBillCents(item), "Regular pays full price");
            equal(example[0], new CriticCustomer(2).calculateBillCents(item), "Critic inherits full-price billing");
        }
        equal(null, vip.getOrder(), "Quoting a bill does not place an order");
        equal(100, vip.getSatisfaction(), "Quoting a bill does not change satisfaction");
        equal(CustomerStatus.WAITING, vip.getStatus(), "Quoting a bill does not change lifecycle state");
    }

    private static void testFoodPreferences() {
        List<MenuItem> menu = baseMenu();
        same(menu.get(1), new Customer(5).chooseItem(menu), "Regular ID 5 chooses Burger");
        same(menu.get(1), new VIPCustomer(5).chooseItem(menu), "VIP inherits regular food selection");
        Customer critic = new CriticCustomer(5);
        same(menu.get(2), critic.chooseItem(menu), "Critic ID 5 chooses Pasta");
        same(menu.get(2), critic.chooseItem(List.of(menu.get(2), menu.get(0), menu.get(1))),
                "Critic choice is independent of menu position");

        MenuItem special = new MenuItem("Soup", 2400, 1);
        List<MenuItem> mutableMenu = new ArrayList<>(menu);
        mutableMenu.add(special);
        List<MenuItem> before = List.copyOf(mutableMenu);
        same(special, critic.chooseItem(mutableMenu), "Critic uses price rather than a dish name");
        equal(before, mutableMenu, "Choosing does not reorder or modify the menu");
        same(special, critic.chooseItem(List.of(special)), "Single-item menu");

        MenuItem tied = new MenuItem("Steak", 2400, 2);
        same(special, critic.chooseItem(List.of(menu.get(0), special, tied)), "First maximum wins a tie");
        same(tied, critic.chooseItem(List.of(tied, special, menu.get(0))), "Tie follows actual menu order");
        MenuItem water = new MenuItem("Water", 0, 1);
        MenuItem tea = new MenuItem("Tea", 0, 1);
        same(water, critic.chooseItem(List.of(water, tea)), "Zero-priced menu still chooses its first maximum");
        equal(null, critic.getOrder(), "Choosing does not place an order");
        equal(100, critic.getSatisfaction(), "Choosing does not change satisfaction");
        equal(CustomerStatus.WAITING, critic.getStatus(), "Choosing does not change lifecycle state");
    }

    private static void testInvalidInputs() {
        for (Customer customer : customers()) {
            throwsType(IllegalArgumentException.class, () -> customer.chooseItem(null), "Null menu");
            throwsType(IllegalArgumentException.class, () -> customer.chooseItem(List.of()), "Empty menu");
            throwsType(IllegalArgumentException.class, () -> customer.calculateBillCents(null), "Null billing item");
            equal(null, customer.getOrder(), "Invalid input does not place an order");
            equal(100, customer.getSatisfaction(), "Invalid input does not change satisfaction");
            equal(CustomerStatus.WAITING, customer.getStatus(), "Invalid input does not change status");
        }
        Customer critic = new CriticCustomer(5);
        MenuItem item = baseMenu().get(0);
        throwsType(IllegalArgumentException.class, () -> critic.chooseItem(Arrays.asList((MenuItem) null)),
                "Null first menu entry");
        throwsType(IllegalArgumentException.class, () -> critic.chooseItem(Arrays.asList(item, null)),
                "Null later menu entry");
        equal(null, critic.getOrder(), "Invalid Critic menu does not place an order");
    }

    private static void testInheritedLifecycle() {
        for (Customer customer : customers()) {
            Order order = new Order(customer.getId(), baseMenu().get(0));
            throwsType(IllegalStateException.class, () -> customer.acceptOrder(order), "Cannot order before seating");
            customer.seat();
            throwsType(IllegalStateException.class, customer::seat, "Cannot seat twice");
            customer.acceptOrder(order);
            same(order, customer.getOrder(), "Inherited shared order reference");
            throwsType(IllegalStateException.class, () -> customer.markServed(1), "Cannot serve unprepared order");
            customer.applyWaitingPenalty();
            int satisfactionAtService = customer.getSatisfaction();
            order.prepareOneUnit();
            order.markServed();
            customer.markServed(1);
            customer.applyWaitingPenalty();
            equal(satisfactionAtService, customer.getSatisfaction(), "Eating has no waiting penalty");
            customer.finishEating(1);
            equal(CustomerStatus.SERVED, customer.getStatus(), "Cannot finish eating on service turn");
            throwsType(IllegalStateException.class, customer::markPaid, "Cannot pay while eating");
            customer.finishEating(2);
            equal(CustomerStatus.READY_TO_PAY, customer.getStatus(), "Inherited eating transition");
            customer.applyWaitingPenalty();
            equal(satisfactionAtService, customer.getSatisfaction(), "Payment-ready has no waiting penalty");
            throwsType(IllegalStateException.class, customer::markPaid, "Order must be marked paid first");
            order.markPaid();
            customer.markPaid();
            equal(CustomerStatus.PAID, customer.getStatus(), "Inherited payment transition");
            equal(1, customer.getServedTurn(), "Service timestamp retained");
            equal(2, customer.getReadyToPayTurn(), "Eating timestamp retained");
            customer.applyWaitingPenalty();
            equal(satisfactionAtService, customer.getSatisfaction(), "Paid has no waiting penalty");
            throwsType(IllegalStateException.class, customer::markPaid, "Cannot pay twice");
            throwsType(IllegalStateException.class, customer::abandon, "Paid customer cannot abandon");
        }
    }

    private static void testInheritedAbandonment() {
        for (boolean seated : new boolean[] {false, true}) {
            for (Customer customer : customers()) {
                if (seated) {
                    customer.seat();
                    customer.acceptOrder(new Order(customer.getId(), baseMenu().get(2)));
                }
                for (int turn = 0; turn < 20; turn++) {
                    customer.applyWaitingPenalty();
                }
                equal(0, customer.getSatisfaction(), "All types eventually reach zero");
                customer.abandon();
                equal(CustomerStatus.LEFT, customer.getStatus(), "Inherited abandonment transition");
                if (seated) {
                    equal(OrderStatus.CANCELLED, customer.getOrder().getStatus(), "Abandonment cancels the order");
                }
                customer.applyWaitingPenalty();
                equal(0, customer.getSatisfaction(), "Departed satisfaction stays unchanged");
                throwsType(IllegalStateException.class, customer::abandon, "Cannot abandon twice");
                throwsType(IllegalStateException.class, customer::seat, "Cannot reseat departed customer");
            }
        }
    }

    private static void same(Object expected, Object actual, String message) {
        if (expected != actual) {
            throw new AssertionError(message + ": expected the same object reference");
        }
    }

    private static void equal(Object expected, Object actual, String message) {
        if (!Objects.equals(expected, actual)) {
            throw new AssertionError(message + ": expected " + expected + ", actual " + actual);
        }
    }

    private static void throwsType(Class<? extends Throwable> expected, Runnable operation, String message) {
        try {
            operation.run();
        } catch (Throwable failure) {
            if (expected.isInstance(failure)) {
                return;
            }
            throw new AssertionError(message + ": unexpected exception", failure);
        }
        throw new AssertionError(message + ": expected " + expected.getSimpleName());
    }
}
