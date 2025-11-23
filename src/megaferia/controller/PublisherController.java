package megaferia.controller;

import core.Manager;
import core.Publisher;
import megaferia.observer.Observer;
import megaferia.observer.Subject;
import megaferia.response.Response;
import megaferia.response.StatusCode;
import megaferia.storage.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;

public class PublisherController implements Subject {

    private final Repository<Publisher, String> publisherRepository;
    private final Repository<Manager, Long> managerRepository;
    private final List<Observer> observers = new ArrayList<>();

    private static final Pattern NIT_PATTERN =
            Pattern.compile("^\\d{3}\\.\\d{3}\\.\\d{3}-\\d$");

    public PublisherController(Repository<Publisher, String> publisherRepository,
                            Repository<Manager, Long> managerRepository) {
        this.publisherRepository = publisherRepository;
        this.managerRepository = managerRepository;
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

    public Response<Publisher> createPublisher(String nitText,
                                            String name,
                                            String address,
                                            String managerIdText) {

        if (nitText == null || nitText.isBlank() ||
            name == null || name.isBlank() ||
            address == null || address.isBlank() ||
            managerIdText == null || managerIdText.isBlank()) {

            return Response.of(StatusCode.BAD_REQUEST,
                    "NIT, nombre, dirección e id de gerente son obligatorios.");
        }

        String nit = nitText.trim();

        // Formato del NIT
        if (!NIT_PATTERN.matcher(nit).matches()) {
            return Response.of(StatusCode.BAD_REQUEST,
                    "El NIT debe tener el formato XXX.XXX.XXX-X.");
        }

        // Unicidad del NIT
        Optional<Publisher> existing = publisherRepository.findById(nit);
        if (existing.isPresent()) {
            return Response.of(StatusCode.CONFLICT,
                    "Ya existe una editorial con ese NIT.");
        }

        // Validar id de gerente
        long managerId;
        try {
            managerId = Long.parseLong(managerIdText.trim());
        } catch (NumberFormatException e) {
            return Response.of(StatusCode.BAD_REQUEST,
                    "El id del gerente debe ser un número entero.");
        }

        if (managerId < 0) {
            return Response.of(StatusCode.BAD_REQUEST,
                    "El id del gerente no puede ser negativo.");
        }

        String managerDigits = managerIdText.trim().replaceFirst("^0+(?!$)", "");
        if (managerDigits.length() > 15) {
            return Response.of(StatusCode.BAD_REQUEST,
                    "El id del gerente no puede tener más de 15 dígitos.");
        }

        // Gerente debe existir previamente
        Optional<Manager> managerOpt = managerRepository.findById(managerId);
        if (managerOpt.isEmpty()) {
            return Response.of(StatusCode.NOT_FOUND,
                    "El gerente con id " + managerId + " no existe.");
        }

        Manager manager = managerOpt.get();

        // Crear y guardar la editorial
        Publisher publisher = new Publisher(
                nit,
                name.trim(),
                address.trim(),
                manager
        );
        publisherRepository.save(publisher);

        // Clon "seguro": clonamos también el manager para no exponer el verdadero
        Manager managerClone = new Manager(
                manager.getId(),
                manager.getFirstname(),
                manager.getLastname()
        );
        Publisher clone = new Publisher(
                publisher.getNit(),
                publisher.getName(),
                publisher.getAddress(),
                managerClone
        );

        notifyObservers("publisher");

        return Response.of(StatusCode.CREATED,
                "Editorial creada correctamente.", clone);
    }

    public Response<List<Publisher>> getAllPublishers() {
        List<Publisher> publishers = publisherRepository.findAll();
        List<Publisher> clones = new ArrayList<>();

        for (Publisher publisher : publishers) {
            Manager manager = publisher.getManager();
            Manager managerClone = new Manager(
                    manager.getId(),
                    manager.getFirstname(),
                    manager.getLastname()
            );
            Publisher clone = new Publisher(
                    publisher.getNit(),
                    publisher.getName(),
                    publisher.getAddress(),
                    managerClone
            );
            clones.add(clone);
        }

        return Response.of(StatusCode.OK, "Listado de editoriales.", clones);
    }
}
