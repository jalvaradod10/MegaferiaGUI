package megaferia.storage;

import core.Author;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

public class AuthorRepository implements Repository<Author, Long> {

    private final List<Author> data = new ArrayList<>();

    @Override
    public Author save(Author entity) {
        data.add(entity);
        sortData();
        return entity;
    }

    @Override
    public Author update(Author entity) {
        for (int i = 0; i < data.size(); i++) {
            if (data.get(i).getId() == entity.getId()) {
                data.set(i, entity);
                sortData();
                return entity;
            }
        }
        return null;
    }

    @Override
    public Optional<Author> findById(Long id) {
        return data.stream()
                .filter(e -> e.getId() == id)
                .findFirst();
    }

    @Override
    public List<Author> findAll() {
        return new ArrayList<>(data);
    }

    private void sortData() {
        data.sort(Comparator.comparingLong(Author::getId));
    }
}
