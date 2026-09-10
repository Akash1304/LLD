package fooddelivery.model;

public class FoodOrderItem {
    private final MenuItem menuItem;
    private final int quantity;

    public FoodOrderItem(MenuItem menuItem, int quantity) {
        this.menuItem = menuItem;
        this.quantity = quantity;
    }

    public MenuItem getMenuItem() { return menuItem; }
    public int getQuantity() { return quantity; }
    public double getLineTotal() { return menuItem.getPrice() * quantity; }

    @Override
    public String toString() { return quantity + "x " + menuItem.getName(); }
}
