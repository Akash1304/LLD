package library.service;

import library.model.Book;
import library.model.BookItem;

import java.util.List;
import java.util.Optional;

public interface CatalogService {
    Book addBook(Book book);
    BookItem addBookItem(BookItem item);
    List<Book> searchByTitle(String query);
    List<Book> searchByAuthor(String query);
    List<BookItem> getItemsForIsbn(String isbn);
    Optional<BookItem> findAvailableItem(String isbn);
    Optional<BookItem> getItemByBarcode(String barcode);
}
