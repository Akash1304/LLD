package bookstore.service;

import bookstore.model.Book;

public class InMemoryInventoryService implements InventoryService {
    private final CatalogService catalogService;

    public InMemoryInventoryService(CatalogService catalogService) {
        this.catalogService = catalogService;
    }

    // Delegates to Book.tryReserve's atomic check-and-decrement instead of
    // synchronizing this whole service: reserving stock for one ISBN
    // should never block a concurrent reservation for a completely
    // different ISBN, which a service-wide lock would do.
    @Override
    public boolean reserveStock(String isbn, int quantity) {
        Book book = catalogService.getBook(isbn).orElseThrow(() -> new IllegalArgumentException("Unknown ISBN: " + isbn));
        return book.tryReserve(quantity);
    }

    @Override
    public void restock(String isbn, int quantity) {
        Book book = catalogService.getBook(isbn).orElseThrow(() -> new IllegalArgumentException("Unknown ISBN: " + isbn));
        book.adjustStock(quantity);
    }

    @Override
    public int getStock(String isbn) {
        return catalogService.getBook(isbn).map(Book::getStock).orElse(0);
    }
}
