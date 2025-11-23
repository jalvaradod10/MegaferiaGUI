package megaferia;

import core.MegaferiaFrame;
import megaferia.controller.BookController;
import megaferia.controller.PersonController;
import megaferia.controller.PublisherController;
import megaferia.controller.StandController;
import megaferia.storage.AuthorRepository;
import megaferia.storage.BookRepository;
import megaferia.storage.ManagerRepository;
import megaferia.storage.NarratorRepository;
import megaferia.storage.PublisherRepository;
import megaferia.storage.StandRepository;

public class MegaferiaMain {

    public static void main(String[] args) {
        StandRepository standRepository = new StandRepository();
        AuthorRepository authorRepository = new AuthorRepository();
        ManagerRepository managerRepository = new ManagerRepository();
        NarratorRepository narratorRepository = new NarratorRepository();
        PublisherRepository publisherRepository = new PublisherRepository();
        BookRepository bookRepository = new BookRepository();

        StandController standController =
                new StandController(standRepository, publisherRepository);

        PersonController personController =
                new PersonController(authorRepository, managerRepository, narratorRepository);

        PublisherController publisherController =
                new PublisherController(publisherRepository, managerRepository);

        BookController bookController =
                new BookController(bookRepository, authorRepository, publisherRepository, narratorRepository);

        java.awt.EventQueue.invokeLater(new Runnable() {
            public void run() {
                MegaferiaFrame frame = new MegaferiaFrame(
                        standController,
                        personController,
                        publisherController,
                        bookController
                );
                frame.setVisible(true);
            }
        });
    }
}
