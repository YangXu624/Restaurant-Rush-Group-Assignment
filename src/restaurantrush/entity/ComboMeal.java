package restaurantrush.entity;

/** Two menu items ordered together at 90% of their combined price. */
public class ComboMeal extends MenuItem {
    private final MenuItem firstItem;
    private final MenuItem secondItem;

    public ComboMeal(MenuItem firstItem, MenuItem secondItem) {
        super(comboName(firstItem, secondItem), comboPriceCents(firstItem, secondItem),
                Math.addExact(firstItem.getPreparationUnits(), secondItem.getPreparationUnits()));
        this.firstItem = firstItem;
        this.secondItem = secondItem;
    }

    private static String comboName(MenuItem first, MenuItem second) {
        if (first == null || second == null) {
            throw new IllegalArgumentException("A combo needs two menu items");
        }
        return first.getName() + " + " + second.getName();
    }

    private static int comboPriceCents(MenuItem first, MenuItem second) {
        long totalCents = (long) first.getPriceCents() + second.getPriceCents();
        // Adding half a cent before integer division rounds to the nearest cent.
        return Math.toIntExact((totalCents * 90 + 50) / 100);
    }

    public MenuItem getFirstItem() {
        return firstItem;
    }

    public MenuItem getSecondItem() {
        return secondItem;
    }
}
