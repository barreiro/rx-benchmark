package org.hibernate.reactive.benchmark;

import io.smallrye.mutiny.Uni;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.reactive.model.Author;
import org.hibernate.reactive.model.Book;
import org.hibernate.reactive.mutiny.Mutiny;
import org.hibernate.reactive.stage.Stage;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.TearDown;

import java.util.Properties;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;

import static javax.persistence.Persistence.createEntityManagerFactory;
import static org.hibernate.cfg.AvailableSettings.JPA_PERSISTENCE_PROVIDER;

@State(Scope.Benchmark)
public class ReactiveBenchmarkState {

    private final ExecutorService executor = Executors.newFixedThreadPool(10); // Same as the pool size defined in persistence.xml

    private Stage.SessionFactory rxSessionFactory;
    private Mutiny.SessionFactory mutinySessionFactory;
    private SessionFactory ormSessionFactory;

    private Integer[] ids;

    @Setup
    public void setup() {
        // obtain a factory for reactive sessions based on the standard JPA configuration properties specified in resources/META-INF/persistence.xml
        Properties properties = new Properties();
        properties.setProperty(JPA_PERSISTENCE_PROVIDER, "org.hibernate.reactive.provider.ReactivePersistenceProvider");
        rxSessionFactory = createEntityManagerFactory("example", properties).unwrap(Stage.SessionFactory.class);
        mutinySessionFactory = createEntityManagerFactory("example", properties).unwrap(Mutiny.SessionFactory.class);

        ormSessionFactory = createEntityManagerFactory("example").unwrap(SessionFactory.class);

        ids = populateDatabase();
    }

    @TearDown
    public void shutdown() {
        cleanUpDatabase();
        rxSessionFactory.close();
        mutinySessionFactory.close();
        ormSessionFactory.close();
        executor.shutdown();
    }

    protected Integer[] populateDatabase() {
        Author author1 = new Author("Iain M. Banks");
        Author author2 = new Author("Neal Stephenson");
        Book book1 = new Book("1-85723-235-6", "Feersum Endjinn", author1);
        Book book2 = new Book("0-380-97346-4", "Cryptonomicon", author2);
        Book book3 = new Book("0-553-08853-X", "Snow Crash", author2);
        author1.addBook(book1);
        author2.addBook(book2);
        author2.addBook(book3);

        inOrmTransaction(
                entityManager -> {
                    entityManager.persist(author1);
                    entityManager.persist(author2);
                }
        );

        return new Integer[]{author1.getId(), author2.getId()};
    }

    protected void cleanUpDatabase() {
        inOrmTransaction(entityManager -> entityManager.createQuery("delete from Book").executeUpdate());
        inOrmTransaction(entityManager -> entityManager.createQuery("delete from Author").executeUpdate());
    }

    public Executor getExecutor() {
        return executor;
    }

    public Integer[] getIds() {
        return ids;
    }

    public Integer getSingleId() {
        return ids[0];
    }

    // --- //

    public <T> CompletionStage<T> withReactiveSession(Function<Stage.Session, CompletionStage<T>> work) {
        return rxSessionFactory.withSession(work);
    }

    public <T> CompletionStage<T> withReactiveTransaction(BiFunction<Stage.Session, Stage.Transaction, CompletionStage<T>> work) {
        return rxSessionFactory.withTransaction(work);
    }

    public <T> Uni<T> withMutinySession(Function<Mutiny.Session, Uni<T>> work) {
        return mutinySessionFactory.withSession(work);
    }

    public <T> Uni<T> withMutinyTransaction(BiFunction<Mutiny.Session, Mutiny.Transaction, Uni<T>> work) {
        return mutinySessionFactory.withTransaction(work);
    }
    
    public void inOrmSession(Consumer<Session> consumer) {
        try (Session s = ormSessionFactory.openSession()) {
            consumer.accept(s);
        }
    }

    public void inOrmTransaction(Consumer<Session> consumer) {
        Session s = ormSessionFactory.openSession();
        try {
            s.getTransaction().begin();
            consumer.accept(s);
            s.getTransaction().commit();
        } catch (Exception e) {
            if (s.getTransaction().isActive()) {
                s.getTransaction().rollback();
            }
        } finally {
            s.close();
        }
    }
}
