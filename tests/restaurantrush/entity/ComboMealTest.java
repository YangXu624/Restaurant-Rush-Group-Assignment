package restaurantrush.entity;

public class ComboMealTest {
    public static void main(String[] args) {
        MenuItem salad = new MenuItem("Salad", 900, 1);
        MenuItem pasta = new MenuItem("Pasta", 1800, 2);
        ComboMeal combo = new ComboMeal(salad, pasta);

        if (combo.getFirstItem() != salad || combo.getSecondItem() != pasta) {
            throw new AssertionError("Combo should contain the original menu items");
        }
        if (combo.getPriceCents() != 2430 || combo.getPreparationUnits() != 3) {
            throw new AssertionError("Salad + Pasta should cost $24.30 and take 3 units");
        }

        MenuItem small = new MenuItem("Small", 3, 1);
        MenuItem side = new MenuItem("Side", 2, 1);
        if (new ComboMeal(small, side).getPriceCents() != 5) {
            throw new AssertionError("90% of 5 cents is 4.5 cents and should round up to 5");
        }
        System.out.println("All ComboMeal tests passed successfully!");
    }
}
