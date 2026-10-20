package restaurantrush.entity;

/**
 * Stage 1 skeleton. Owner: M1: Yang Xu.
 * See docs/class-contracts.md for the implementation contract.
 */
public class MenuItem {
    private final String name;
    private final int priceCents;
    private final int preparationUnits;

    public MenuItem(String name, int priceCents, int preparationUnits) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Menu item name cannot be null or blank");
        }
        if (priceCents < 0) {
            throw new IllegalArgumentException("Menu item price cannot be negative");
        }
        if (preparationUnits <= 0) {
            throw new IllegalArgumentException("Menu item preparation units must be positive");
        }
        this.name = name;
        this.priceCents = priceCents;
        this.preparationUnits = preparationUnits;
    }

    public int getPriceCents() {
        return priceCents;
    }

    public int getPreparationUnits() {
        return preparationUnits;
    }

    public String getName() {
        return name;
    }
}
