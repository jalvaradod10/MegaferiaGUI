package megaferia.controller;

import core.Author;
import core.Audiobook;
import core.Book;
import core.DigitalBook;
import core.Manager;
import core.Narrator;
import core.PrintedBook;
import core.Publisher;
import megaferia.observer.Observer;
import megaferia.observer.Subject;
import megaferia.response.Response;
import megaferia.response.StatusCode;
import megaferia.storage.Repository;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;

public class BookController implements Subject {

    private final Repository<Book, String> bookRepository;
    private final Repository<Author, Long> authorRepository;
    private final Repository<Publisher, String> publisherRepository;
    private final Repository<Narrator, Long> narratorRepository;
    private final List<Observer> observers = new ArrayList<>();

    private static final Pattern ISBN_PATTERN =
            Pattern.compile("^\\d{3}-\\d-\\d{2}-\\d{6}-\\d$");

    public BookController(Repository<Book, String> bookRepository,
                        Repository<Author, Long> authorRepository,
                        Repository<Publisher, String> publisherRepository,
                        Repository<Narrator, Long> narratorRepository) {
        this.bookRepository = bookRepository;
        this.authorRepository = authorRepository;
        this.publisherRepository = publisherRepository;
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


    private static class CommonBookData {
        String title;
        List<Author> authors;
        String isbn;
        String genre;
        String format;
        double value;
        Publisher publisher;
    }


    public Response<Book> createPrintedBook(String title,
                                            List<Long> authorIds,
                                            String isbnText,
                                            String genre,
                                            String format,
                                            String valueText,
                                            String publisherNit,
                                            String pagesText,
                                            String copiesText) {

        Response<CommonBookData> commonValidation =
                validateCommonBookData(title, authorIds, isbnText, genre, format,
                        valueText, publisherNit);

        if (!commonValidation.isSuccess()) {
            return Response.of(commonValidation.getStatus(), commonValidation.getMessage());
        }

        CommonBookData data = commonValidation.getData();

        int pages;
        int copies;
        try {
            pages = Integer.parseInt(pagesText.trim());
            copies = Integer.parseInt(copiesText.trim());
        } catch (Exception e) {
            return Response.of(StatusCode.BAD_REQUEST,
                    "Páginas y número de ejemplares deben ser números enteros.");
        }

        if (pages <= 0 || copies <= 0) {
            return Response.of(StatusCode.BAD_REQUEST,
                    "Páginas y número de ejemplares deben ser mayores que cero.");
        }

        PrintedBook printedBook = new PrintedBook(
                data.title,
                new ArrayList<>(data.authors),
                data.isbn,
                data.genre,
                data.format,
                data.value,
                data.publisher,
                pages,
                copies
        );

        bookRepository.save(printedBook);

        Book clone = cloneBook(printedBook);

        notifyObservers("book");

        return Response.of(StatusCode.CREATED,
                "Libro impreso creado correctamente.", clone);
    }


    public Response<Book> createDigitalBook(String title,
                                            List<Long> authorIds,
                                            String isbnText,
                                            String genre,
                                            String format,
                                            String valueText,
                                            String publisherNit,
                                            String hyperlink) {

        Response<CommonBookData> commonValidation =
                validateCommonBookData(title, authorIds, isbnText, genre, format,
                        valueText, publisherNit);

        if (!commonValidation.isSuccess()) {
            return Response.of(commonValidation.getStatus(), commonValidation.getMessage());
        }

        CommonBookData data = commonValidation.getData();

        DigitalBook digitalBook;
        if (hyperlink == null || hyperlink.isBlank()) {
            digitalBook = new DigitalBook(
                    data.title,
                    new ArrayList<>(data.authors),
                    data.isbn,
                    data.genre,
                    data.format,
                    data.value,
                    data.publisher
            );
        } else {
            digitalBook = new DigitalBook(
                    data.title,
                    new ArrayList<>(data.authors),
                    data.isbn,
                    data.genre,
                    data.format,
                    data.value,
                    data.publisher,
                    hyperlink.trim()
            );
        }

        bookRepository.save(digitalBook);

        Book clone = cloneBook(digitalBook);

        notifyObservers("book");

        return Response.of(StatusCode.CREATED,
                "Libro digital creado correctamente.", clone);
    }


    public Response<Book> createAudiobook(String title,
                                        List<Long> authorIds,
                                        String isbnText,
                                        String genre,
                                        String format,
                                        String valueText,
                                        String publisherNit,
                                        String narratorIdText,
                                        String durationText) {

        Response<CommonBookData> commonValidation =
                validateCommonBookData(title, authorIds, isbnText, genre, format,
                        valueText, publisherNit);

        if (!commonValidation.isSuccess()) {
            return Response.of(commonValidation.getStatus(), commonValidation.getMessage());
        }

        CommonBookData data = commonValidation.getData();

        // Validar narrador
        if (narratorIdText == null || narratorIdText.isBlank()) {
            return Response.of(StatusCode.BAD_REQUEST,
                    "El id del narrador es obligatorio.");
        }

        long narratorId;
        try {
            narratorId = Long.parseLong(narratorIdText.trim());
        } catch (NumberFormatException e) {
            return Response.of(StatusCode.BAD_REQUEST,
                    "El id del narrador debe ser un número entero.");
        }

        if (narratorId < 0) {
            return Response.of(StatusCode.BAD_REQUEST,
                    "El id del narrador no puede ser negativo.");
        }

        String narratorDigits = narratorIdText.trim().replaceFirst("^0+(?!$)", "");
        if (narratorDigits.length() > 15) {
            return Response.of(StatusCode.BAD_REQUEST,
                    "El id del narrador no puede tener más de 15 dígitos.");
        }

        Optional<Narrator> narratorOpt = narratorRepository.findById(narratorId);
        if (narratorOpt.isEmpty()) {
            return Response.of(StatusCode.NOT_FOUND,
                    "El narrador con id " + narratorId + " no existe.");
        }
        Narrator narrator = narratorOpt.get();

        int duration;
        try {
            duration = Integer.parseInt(durationText.trim());
        } catch (Exception e) {
            return Response.of(StatusCode.BAD_REQUEST,
                    "La duración debe ser un número entero (minutos, por ejemplo).");
        }

        if (duration <= 0) {
            return Response.of(StatusCode.BAD_REQUEST,
                    "La duración debe ser mayor que cero.");
        }

        Audiobook audiobook = new Audiobook(
                data.title,
                new ArrayList<>(data.authors),
                data.isbn,
                data.genre,
                data.format,
                data.value,
                data.publisher,
                duration,
                narrator
        );

        bookRepository.save(audiobook);

        Book clone = cloneBook(audiobook);

        notifyObservers("book");

        return Response.of(StatusCode.CREATED,
                "Audiolibro creado correctamente.", clone);
    }


    public Response<List<Book>> getBooksByType(String type) {
        List<Book> all = bookRepository.findAll(); // ya ordenados por ISBN
        List<Book> result = new ArrayList<>();

        for (Book book : all) {
            boolean add = false;

            if ("Libros Impresos".equals(type) && book instanceof PrintedBook) {
                add = true;
            } else if ("Libros Digitales".equals(type) && book instanceof DigitalBook) {
                add = true;
            } else if ("Audiolibros".equals(type) && book instanceof Audiobook) {
                add = true;
            } else if ("Todos los Libros".equals(type)) {
                add = true;
            }

            if (add) {
                result.add(cloneBook(book));
            }
        }

        return Response.of(StatusCode.OK, "Libros filtrados por tipo.", result);
    }

    public Response<List<Book>> getBooksByAuthor(long authorId) {
        Optional<Author> authorOpt = authorRepository.findById(authorId);
        if (authorOpt.isEmpty()) {
            return Response.of(StatusCode.NOT_FOUND,
                    "El autor con id " + authorId + " no existe.");
        }

        List<Book> all = bookRepository.findAll();
        List<Book> result = new ArrayList<>();

        for (Book book : all) {
            for (Author author : book.getAuthors()) {
                if (author.getId() == authorId) {
                    result.add(cloneBook(book));
                    break;
                }
            }
        }

        return Response.of(StatusCode.OK,
                "Libros del autor con id " + authorId + ".", result);
    }

    public Response<List<Book>> getBooksByFormat(String format) {
        if (format == null || format.isBlank()) {
            return Response.of(StatusCode.BAD_REQUEST,
                    "El formato es obligatorio.");
        }

        List<Book> all = bookRepository.findAll();
        List<Book> result = new ArrayList<>();

        for (Book book : all) {
            if (format.equals(book.getFormat())) {
                result.add(cloneBook(book));
            }
        }

        return Response.of(StatusCode.OK,
                "Libros filtrados por formato.", result);
    }

    public Response<List<Author>> getAuthorsWithMostDifferentPublishers() {
        List<Author> authors = authorRepository.findAll();

        if (authors.isEmpty()) {
            return Response.of(StatusCode.OK,
                    "No hay autores registrados.", new ArrayList<>());
        }

        int maxPublishers = -1;
        List<Author> maxAuthors = new ArrayList<>();

        for (Author author : authors) {
            int qty = author.getPublisherQuantity();
            if (qty > maxPublishers) {
                maxPublishers = qty;
                maxAuthors.clear();
                maxAuthors.add(author);
            } else if (qty == maxPublishers) {
                maxAuthors.add(author);
            }
        }

        maxAuthors.sort(Comparator.comparingLong(Author::getId));

        List<Author> clones = new ArrayList<>();
        for (Author author : maxAuthors) {
            clones.add(new Author(
                    author.getId(),
                    author.getFirstname(),
                    author.getLastname()
            ));
        }

        return Response.of(StatusCode.OK,
                "Autores con más libros en diferentes editoriales: " + maxPublishers,
                clones);
    }


    private Response<CommonBookData> validateCommonBookData(String title,
                                                            List<Long> authorIds,
                                                            String isbnText,
                                                            String genre,
                                                            String format,
                                                            String valueText,
                                                            String publisherNit) {

        if (title == null || title.isBlank()) {
            return Response.of(StatusCode.BAD_REQUEST,
                    "El título del libro es obligatorio.");
        }

        if (authorIds == null || authorIds.isEmpty()) {
            return Response.of(StatusCode.BAD_REQUEST,
                    "Debe seleccionar al menos un autor.");
        }

        List<Author> authors = new ArrayList<>();
        for (Long id : authorIds) {
            if (id == null) continue;

            for (Author existing : authors) {
                if (existing.getId() == id) {
                    return Response.of(StatusCode.BAD_REQUEST,
                            "No se puede repetir un autor en el mismo libro.");
                }
            }

            Optional<Author> authorOpt = authorRepository.findById(id);
            if (authorOpt.isEmpty()) {
                return Response.of(StatusCode.NOT_FOUND,
                        "El autor con id " + id + " no existe.");
            }
            authors.add(authorOpt.get());
        }

        if (isbnText == null || isbnText.isBlank()) {
            return Response.of(StatusCode.BAD_REQUEST,
                    "El ISBN es obligatorio.");
        }

        String isbn = isbnText.trim();

        if (!ISBN_PATTERN.matcher(isbn).matches()) {
            return Response.of(StatusCode.BAD_REQUEST,
                    "El ISBN debe tener el formato XXX-X-XX-XXXXXX-X.");
        }

        if (bookRepository.findById(isbn).isPresent()) {
            return Response.of(StatusCode.CONFLICT,
                    "Ya existe un libro con ese ISBN.");
        }

        if (genre == null || genre.isBlank()) {
            return Response.of(StatusCode.BAD_REQUEST,
                    "El género del libro es obligatorio.");
        }

        if (format == null || format.isBlank()) {
            return Response.of(StatusCode.BAD_REQUEST,
                    "El formato del libro es obligatorio.");
        }

        if (valueText == null || valueText.isBlank()) {
            return Response.of(StatusCode.BAD_REQUEST,
                    "El valor del libro es obligatorio.");
        }

        double value;
        try {
            value = Double.parseDouble(valueText.trim());
        } catch (NumberFormatException e) {
            return Response.of(StatusCode.BAD_REQUEST,
                    "El valor del libro debe ser un número.");
        }

        if (value <= 0) {
            return Response.of(StatusCode.BAD_REQUEST,
                    "El valor del libro debe ser mayor que cero.");
        }

        if (publisherNit == null || publisherNit.isBlank()) {
            return Response.of(StatusCode.BAD_REQUEST,
                    "El NIT de la editorial es obligatorio.");
        }

        String nit = publisherNit.trim();
        Optional<Publisher> publisherOpt = publisherRepository.findById(nit);
        if (publisherOpt.isEmpty()) {
            return Response.of(StatusCode.NOT_FOUND,
                    "La editorial con NIT " + nit + " no existe.");
        }

        Publisher publisher = publisherOpt.get();

        CommonBookData data = new CommonBookData();
        data.title = title.trim();
        data.authors = authors;
        data.isbn = isbn;
        data.genre = genre.trim();
        data.format = format.trim();
        data.value = value;
        data.publisher = publisher;

        return Response.of(StatusCode.OK, "Datos del libro válidos.", data);
    }

    private Book cloneBook(Book book) {
        // Clonar autores
        List<Author> authorClones = new ArrayList<>();
        for (Author author : book.getAuthors()) {
            authorClones.add(new Author(
                    author.getId(),
                    author.getFirstname(),
                    author.getLastname()
            ));
        }

        Publisher publisher = book.getPublisher();
        Manager managerClone = new Manager(
                publisher.getManager().getId(),
                publisher.getManager().getFirstname(),
                publisher.getManager().getLastname()
        );
        Publisher publisherClone = new Publisher(
                publisher.getNit(),
                publisher.getName(),
                publisher.getAddress(),
                managerClone
        );

        if (book instanceof PrintedBook printed) {
            return new PrintedBook(
                    printed.getTitle(),
                    new ArrayList<>(authorClones),
                    printed.getIsbn(),
                    printed.getGenre(),
                    printed.getFormat(),
                    printed.getValue(),
                    publisherClone,
                    printed.getPages(),
                    printed.getCopies()
            );
        }

        if (book instanceof DigitalBook digital) {
            if (digital.hasHyperlink()) {
                return new DigitalBook(
                        digital.getTitle(),
                        new ArrayList<>(authorClones),
                        digital.getIsbn(),
                        digital.getGenre(),
                        digital.getFormat(),
                        digital.getValue(),
                        publisherClone,
                        digital.getHyperlink()
                );
            } else {
                return new DigitalBook(
                        digital.getTitle(),
                        new ArrayList<>(authorClones),
                        digital.getIsbn(),
                        digital.getGenre(),
                        digital.getFormat(),
                        digital.getValue(),
                        publisherClone
                );
            }
        }

        if (book instanceof Audiobook audio) {
            Narrator n = audio.getNarrador();
            Narrator narratorClone = new Narrator(
                    n.getId(),
                    n.getFirstname(),
                    n.getLastname()
            );
            return new Audiobook(
                    audio.getTitle(),
                    new ArrayList<>(authorClones),
                    audio.getIsbn(),
                    audio.getGenre(),
                    audio.getFormat(),
                    audio.getValue(),
                    publisherClone,
                    audio.getDuration(),
                    narratorClone
            );
        }

        return null;
    }
}
