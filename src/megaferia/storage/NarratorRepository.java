package megaferia.storage;

import core.Narrator;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

public class NarratorRepository implements Repository<Narrator, Long> {

    private final List<Narrator> data = new ArrayList<>();

    @Override
    public Narrator save(Narrator entity) {
        data.add(entity);
        sortData();
        return entity;
    }

    @Override
    public Narrator update(Narrator entity) {
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
    public Optional<Narrator> findById(Long id) {
        return data.stream()
                .filter(e -> e.getId() == id)
                .findFirst();
    }

    @Override
    public List<Narrator> findAll() {
        return new ArrayList<>(data);
    }

    private void sortData() {
        data.sort(Comparator.comparingLong(Narrator::getId));
    }
}
