package megaferia.storage;

import core.Book;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

public class BookRepository implements Repository<Book, String> {

    private final List<Book> data = new ArrayList<>();

    @Override
    public Book save(Book entity) {
        data.add(entity);
        sortData();
        return entity;
    }

    @Override
    public Book update(Book entity) {
        for (int i = 0; i < data.size(); i++) {
            if (data.get(i).getIsbn().equals(entity.getIsbn())) {
                data.set(i, entity);
                sortData();
                return entity;
            }
        }
        return null;
    }

    @Override
    public Optional<Book> findById(String isbn) {
        return data.stream()
                .filter(e -> e.getIsbn().equals(isbn))
                .findFirst();
    }

    @Override
    public List<Book> findAll() {
        return new ArrayList<>(data);
    }

    private void sortData() {
        data.sort(Comparator.comparing(Book::getIsbn));
    }
}
