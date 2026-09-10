package shoppingcart.model;

public class CartItem {
    private final Product product;
    private int quantity;

    public CartItem(Product product, int quantity) {
        this.product = product;
        this.quantity = quantity;
    }

    public Product getProduct() { return product; }
    public int getQuantity() { return quantity; }
    // package-private: mutation only ever happens through Cart's own
    // synchronized methods (same package), so a caller holding a CartItem
    // from Cart.getItems() can't bypass the cart's lock and mutate a line
    // item out from under a concurrent cart edit.
    void setQuantity(int quantity) { this.quantity = quantity; }
    public double getLineTotal() { return product.getPrice() * quantity; }

    @Override
    public String toString() { return quantity + "x " + product.getName() + " = $" + getLineTotal(); }
}
