package megaferia.storage;

import core.Stand;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

public class StandRepository implements Repository<Stand, Long> {

    private final List<Stand> data = new ArrayList<>();

    @Override
    public Stand save(Stand entity) {
        data.add(entity);
        sortData();
        return entity;
    }

    @Override
    public Stand update(Stand entity) {
        for (int i = 0; i < data.size(); i++) {
            if (data.get(i).getId() == entity.getId()) {
                data.set(i, entity);
                sortData();
                return entity;
            }
        }
        return null; // el controlador decidirá qué hacer si no lo encuentra
    }

    @Override
    public Optional<Stand> findById(Long id) {
        return data.stream()
                .filter(e -> e.getId() == id)
                .findFirst();
    }

    @Override
    public List<Stand> findAll() {
        // devolvemos una copia para no exponer la lista interna
        return new ArrayList<>(data);
    }

    private void sortData() {
        // requisito del parcial: stands ordenados por id
        data.sort(Comparator.comparingLong(Stand::getId));
    }
}
