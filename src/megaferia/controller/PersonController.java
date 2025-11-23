package megaferia.controller;

import core.Author;
import core.Manager;
import core.Narrator;
import megaferia.observer.Observer;
import megaferia.observer.Subject;
import megaferia.response.Response;
import megaferia.response.StatusCode;
import megaferia.storage.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class PersonController implements Subject {

    private final Repository<Author, Long> authorRepository;
    private final Repository<Manager, Long> managerRepository;
    private final Repository<Narrator, Long> narratorRepository;
    private final List<Observer> observers = new ArrayList<>();

    public PersonController(Repository<Author, Long> authorRepository,
                            Repository<Manager, Long> managerRepository,
                            Repository<Narrator, Long> narratorRepository) {
        this.authorRepository = authorRepository;
        this.managerRepository = managerRepository;
        this.narratorRepository = narratorRepository;
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


    public Response<Author> createAuthor(String idText, String firstname, String lastname) {
        Response<Long> idValidation = validatePersonId(idText);
        if (!idValidation.isSuccess()) {
            return Response.of(idValidation.getStatus(), idValidation.getMessage());
        }
        long id = idValidation.getData();

        if (firstname == null || firstname.isBlank() ||
            lastname == null || lastname.isBlank()) {
            return Response.of(StatusCode.BAD_REQUEST, "El nombre y apellido del autor son obligatorios.");
        }

        if (existsPersonId(id)) {
            return Response.of(StatusCode.CONFLICT, "Ya existe una persona con ese id.");
        }

        Author author = new Author(id, firstname.trim(), lastname.trim());
        authorRepository.save(author);

        Author clone = new Author(author.getId(), author.getFirstname(), author.getLastname());

        notifyObservers("author");

        return Response.of(StatusCode.CREATED, "Autor creado correctamente.", clone);
    }


    public Response<Manager> createManager(String idText, String firstname, String lastname) {
        Response<Long> idValidation = validatePersonId(idText);
        if (!idValidation.isSuccess()) {
            return Response.of(idValidation.getStatus(), idValidation.getMessage());
        }
        long id = idValidation.getData();

        if (firstname == null || firstname.isBlank() ||
            lastname == null || lastname.isBlank()) {
            return Response.of(StatusCode.BAD_REQUEST, "El nombre y apellido del gerente son obligatorios.");
        }

        if (existsPersonId(id)) {
            return Response.of(StatusCode.CONFLICT, "Ya existe una persona con ese id.");
        }

        Manager manager = new Manager(id, firstname.trim(), lastname.trim());
        managerRepository.save(manager);

        Manager clone = new Manager(manager.getId(), manager.getFirstname(), manager.getLastname());

        notifyObservers("manager");

        return Response.of(StatusCode.CREATED, "Gerente creado correctamente.", clone);
    }


    public Response<Narrator> createNarrator(String idText, String firstname, String lastname) {
        Response<Long> idValidation = validatePersonId(idText);
        if (!idValidation.isSuccess()) {
            return Response.of(idValidation.getStatus(), idValidation.getMessage());
        }
        long id = idValidation.getData();

        if (firstname == null || firstname.isBlank() ||
            lastname == null || lastname.isBlank()) {
            return Response.of(StatusCode.BAD_REQUEST, "El nombre y apellido del narrador son obligatorios.");
        }

        if (existsPersonId(id)) {
            return Response.of(StatusCode.CONFLICT, "Ya existe una persona con ese id.");
        }

        Narrator narrator = new Narrator(id, firstname.trim(), lastname.trim());
        narratorRepository.save(narrator);

        Narrator clone = new Narrator(narrator.getId(), narrator.getFirstname(), narrator.getLastname());

        notifyObservers("narrator");

        return Response.of(StatusCode.CREATED, "Narrador creado correctamente.", clone);
    }

    public Response<List<Author>> getAllAuthors() {
        List<Author> authors = authorRepository.findAll();
        List<Author> clones = new ArrayList<>();
        for (Author author : authors) {
            clones.add(new Author(author.getId(), author.getFirstname(), author.getLastname()));
        }
        return Response.of(StatusCode.OK, "Listado de autores", clones);
    }

    public Response<List<Manager>> getAllManagers() {
        List<Manager> managers = managerRepository.findAll();
        List<Manager> clones = new ArrayList<>();
        for (Manager manager : managers) {
            clones.add(new Manager(manager.getId(), manager.getFirstname(), manager.getLastname()));
        }
        return Response.of(StatusCode.OK, "Listado de gerentes", clones);
    }

    public Response<List<Narrator>> getAllNarrators() {
        List<Narrator> narrators = narratorRepository.findAll();
        List<Narrator> clones = new ArrayList<>();
        for (Narrator narrator : narrators) {
            clones.add(new Narrator(narrator.getId(), narrator.getFirstname(), narrator.getLastname()));
        }
        return Response.of(StatusCode.OK, "Listado de narradores", clones);
    }

    private Response<Long> validatePersonId(String idText) {
        if (idText == null || idText.isBlank()) {
            return Response.of(StatusCode.BAD_REQUEST, "El id de la persona es obligatorio.");
        }

        String trimmed = idText.trim();
        long id;
        try {
            id = Long.parseLong(trimmed);
        } catch (NumberFormatException e) {
            return Response.of(StatusCode.BAD_REQUEST, "El id de la persona debe ser un número entero.");
        }

        if (id < 0) {
            return Response.of(StatusCode.BAD_REQUEST, "El id de la persona no puede ser negativo.");
        }

        String digitsOnly = trimmed.replaceFirst("^0+(?!$)", "");
        if (digitsOnly.length() > 15) {
            return Response.of(StatusCode.BAD_REQUEST, "El id de la persona no puede tener más de 15 dígitos.");
        }

        return Response.of(StatusCode.OK, "Id válido.", id);
    }


    private boolean existsPersonId(long id) {
        Optional<Author> a = authorRepository.findById(id);
        if (a.isPresent()) return true;
        Optional<Manager> m = managerRepository.findById(id);
        if (m.isPresent()) return true;
        Optional<Narrator> n = narratorRepository.findById(id);
        return n.isPresent();
    }
}
