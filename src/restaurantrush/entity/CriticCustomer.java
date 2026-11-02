package restaurantrush.entity;

import java.util.List;

/** Critic behavior for Stage 2; lifecycle and state remain in Customer. */
public class CriticCustomer extends Customer {
    public CriticCustomer(int id) {
        super(id);
    }

    @Override
    protected int waitingPenaltyPerTurn() {
        return 12;
    }

    /** Chooses the highest-priced item; the first item wins a price tie. */
    @Override
    public MenuItem chooseItem(List<MenuItem> menu) {
        if (menu == null || menu.isEmpty()) {
            throw new IllegalArgumentException("Menu cannot be null or empty");
        }
        MenuItem mostExpensive = null;
        for (MenuItem item : menu) {
            if (item == null) {
                throw new IllegalArgumentException("Menu item cannot be null");
            }
            if (mostExpensive == null || item.getPriceCents() > mostExpensive.getPriceCents()) {
                mostExpensive = item;
            }
        }
        return mostExpensive;
    }

    @Override
    public String getCustomerType() {
        return "Critic";
    }
}
