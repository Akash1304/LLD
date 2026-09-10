package bookstore.service;

public interface InventoryService {
    boolean reserveStock(String isbn, int quantity);
    void restock(String isbn, int quantity);
    int getStock(String isbn);
}
