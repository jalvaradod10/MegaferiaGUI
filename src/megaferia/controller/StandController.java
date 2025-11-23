package megaferia.controller;

import core.Stand;
import core.Publisher;
import megaferia.observer.Observer;
import megaferia.observer.Subject;
import megaferia.response.Response;
import megaferia.response.StatusCode;
import megaferia.storage.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class StandController implements Subject {

    private final Repository<Stand, Long> standRepository;
    private final Repository<Publisher, String> publisherRepository;
    private final List<Observer> observers = new ArrayList<>();

    public StandController(Repository<Stand, Long> standRepository,
        Repository<Publisher, String> publisherRepository) {
        this.standRepository = standRepository;
        this.publisherRepository = publisherRepository;
    }

    @Override
    public void registerObserver(Observer observer) {
        if (observer != null && !observers.contains(observer)) {
            observers.add(observer);
        }
    }

    @Override
    public void removeObserver(Observer observer) {
        observers.remove(observer);
    }

    @Override
    public void notifyObservers(String dataType) {
        for (Observer observer : observers) {
            observer.update(dataType);
        }
    }

    public Response<Stand> createStand(String idText, String priceText) {
        if (idText == null || idText.isBlank() || priceText == null || priceText.isBlank()) {
            return Response.of(StatusCode.BAD_REQUEST, "El id y el precio del stand son obligatorios.");
        }

        long id;
        try {
            id = Long.parseLong(idText.trim());
        } catch (NumberFormatException e) {
            return Response.of(StatusCode.BAD_REQUEST, "El id del stand debe ser un número entero.");
        }

        if (id < 0) {
            return Response.of(StatusCode.BAD_REQUEST, "El id del stand no puede ser negativo.");
        }

        // Máximo 15 dígitos
        String digitsOnly = idText.trim().replaceFirst("^0+(?!$)", "");
        if (digitsOnly.length() > 15) {
            return Response.of(StatusCode.BAD_REQUEST, "El id del stand no puede tener más de 15 dígitos.");
        }

        double price;
        try {
            price = Double.parseDouble(priceText.trim());
        } catch (NumberFormatException e) {
            return Response.of(StatusCode.BAD_REQUEST, "El precio del stand debe ser un número.");
        }

        if (price <= 0) {
            return Response.of(StatusCode.BAD_REQUEST, "El precio del stand debe ser mayor que cero.");
        }

        Optional<Stand> existing = standRepository.findById(id);
        if (existing.isPresent()) {
            return Response.of(StatusCode.CONFLICT, "Ya existe un stand con ese id.");
        }

        Stand stand = new Stand(id, price);
        standRepository.save(stand);

        Stand clone = new Stand(stand.getId(), stand.getPrice());

        notifyObservers("stand");

        return Response.of(StatusCode.CREATED, "Stand creado correctamente.", clone);
    }

    public Response<List<Stand>> getAllStands() {
        List<Stand> stands = standRepository.findAll();
        List<Stand> clones = new ArrayList<>();
        for (Stand stand : stands) {
            clones.add(new Stand(stand.getId(), stand.getPrice()));
        }
        return Response.of(StatusCode.OK, "Listado de stands", clones);
    }


    public Response<Void> buyStands(List<Long> standIds, List<String> publisherNits) {
        if (standIds == null || standIds.isEmpty()) {
            return Response.of(StatusCode.BAD_REQUEST, "Debe seleccionar al menos un stand.");
        }
        if (publisherNits == null || publisherNits.isEmpty()) {
            return Response.of(StatusCode.BAD_REQUEST, "Debe seleccionar al menos una editorial.");
        }


        List<Stand> stands = new ArrayList<>();
        for (Long id : standIds) {
            if (id == null) continue;
            Optional<Stand> optionalStand = standRepository.findById(id);
            if (optionalStand.isEmpty()) {
                return Response.of(StatusCode.NOT_FOUND, "No existe el stand con id " + id + ".");
            }
            stands.add(optionalStand.get());
        }


        List<Publisher> publishers = new ArrayList<>();
        for (String nit : publisherNits) {
            if (nit == null || nit.isBlank()) continue;
            Optional<Publisher> optionalPublisher = publisherRepository.findById(nit.trim());
            if (optionalPublisher.isEmpty()) {
                return Response.of(StatusCode.NOT_FOUND, "No existe la editorial con NIT " + nit + ".");
            }
            publishers.add(optionalPublisher.get());
        }

        // Repetidos dentro de la selección
        if (hasDuplicates(standIds)) {
            return Response.of(StatusCode.BAD_REQUEST, "No puede haber stands repetidos en la compra.");
        }
        if (hasDuplicates(publisherNits)) {
            return Response.of(StatusCode.BAD_REQUEST, "No puede haber editoriales repetidas en la compra.");
        }
        for (Stand stand : stands) {
            for (Publisher publisher : publishers) {
                if (!stand.getPublishers().contains(publisher)) {
                    stand.addPublisher(publisher);
                }
                publisher.addStand(stand);
            }
        }

        notifyObservers("stand");
        notifyObservers("publisher");

        return Response.of(StatusCode.OK, "Compra de stands registrada correctamente.");
    }

    private boolean hasDuplicates(List<?> list) {
        return list.stream().distinct().count() != list.size();
    }
}
