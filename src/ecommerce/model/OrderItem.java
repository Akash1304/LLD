package ecommerce.model;

public class OrderItem {
    private final Product product;
    private final int quantity;
    private final double unitPrice;

    public OrderItem(Product product, int quantity) {
        this.product = product;
        this.quantity = quantity;
        this.unitPrice = product.getPrice();
    }

    public Product getProduct() { return product; }
    public int getQuantity() { return quantity; }
    public double getUnitPrice() { return unitPrice; }
    public double getLineTotal() { return unitPrice * quantity; }

    @Override
    public String toString() { return quantity + "x " + product.getName() + " @ $" + unitPrice; }
}
