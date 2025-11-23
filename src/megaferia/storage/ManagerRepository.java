package megaferia.storage;

import core.Manager;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

public class ManagerRepository implements Repository<Manager, Long> {

    private final List<Manager> data = new ArrayList<>();

    @Override
    public Manager save(Manager entity) {
        data.add(entity);
        sortData();
        return entity;
    }

    @Override
    public Manager update(Manager entity) {
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
    public Optional<Manager> findById(Long id) {
        return data.stream()
                .filter(e -> e.getId() == id)
                .findFirst();
    }

    @Override
    public List<Manager> findAll() {
        return new ArrayList<>(data);
    }

    private void sortData() {
        data.sort(Comparator.comparingLong(Manager::getId));
    }
}
