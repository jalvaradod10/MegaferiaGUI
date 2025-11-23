package megaferia.storage;

import core.Publisher;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

public class PublisherRepository implements Repository<Publisher, String> {

    private final List<Publisher> data = new ArrayList<>();

    @Override
    public Publisher save(Publisher entity) {
        data.add(entity);
        sortData();
        return entity;
    }

    @Override
    public Publisher update(Publisher entity) {
        for (int i = 0; i < data.size(); i++) {
            if (data.get(i).getNit().equals(entity.getNit())) {
                data.set(i, entity);
                sortData();
                return entity;
            }
        }
        return null;
    }

    @Override
    public Optional<Publisher> findById(String nit) {
        return data.stream()
                .filter(e -> e.getNit().equals(nit))
                .findFirst();
    }

    @Override
    public List<Publisher> findAll() {
        return new ArrayList<>(data);
    }

    private void sortData() {
        data.sort(Comparator.comparing(Publisher::getNit));
    }
}
