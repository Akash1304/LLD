package bookstore.service;

import bookstore.model.Book;
import bookstore.specification.BookSpecification;

import java.util.List;
import java.util.Optional;

public interface CatalogService {
    Book addBook(Book book);
    Optional<Book> getBook(String isbn);
    List<Book> search(BookSpecification spec);
    List<Book> listAll();
}
