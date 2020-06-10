package org.hibernate.reactive.benchmark;

import io.smallrye.mutiny.Uni;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.reactive.model.Author;
import org.hibernate.reactive.model.Book;
import org.hibernate.reactive.mutiny.Mutiny;
import org.hibernate.reactive.stage.Stage;
import org.jboss.threads.EnhancedQueueExecutor;
import org.openjdk.jmh.annotations.*;

import java.util.Properties;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.BiFunction;
import java.util.function.Function;

import static javax.persistence.Persistence.createEntityManagerFactory;
import static org.hibernate.cfg.AvailableSettings.JPA_PERSISTENCE_PROVIDER;

@State(Scope.Benchmark)
public class ReactiveBenchmarkState {

    // Number of threads on the dispatch executor should be roughly twice the number of cores.
    @Param({"4"})
    private int dispatchSize;

    private ExecutorService workerExecutor;
    private ExecutorService dispatchExecutor;

    private Stage.SessionFactory rxSessionFactory;
    private Mutiny.SessionFactory mutinySessionFactory;
    private SessionFactory ormSessionFactory;

    private Integer[] ids;



    @Setup(Level.Trial)
    public void setup() {
        // obtain a factory for reactive sessions based on the standard JPA configuration properties specified in resources/META-INF/persistence.xml
        Properties properties = new Properties();
        properties.setProperty(JPA_PERSISTENCE_PROVIDER, "org.hibernate.reactive.provider.ReactivePersistenceProvider");
        rxSessionFactory = createEntityManagerFactory("example", properties).unwrap(Stage.SessionFactory.class);
        mutinySessionFactory = createEntityManagerFactory("example", properties).unwrap(Mutiny.SessionFactory.class);

        ormSessionFactory = createEntityManagerFactory("example").unwrap(SessionFactory.class);

        ids = populateDatabase();
    }

    @Setup(Level.Iteration)
    public void setupExecutors() {
        workerExecutor = new EnhancedQueueExecutor.Builder().build();
        dispatchExecutor = Executors.newFixedThreadPool(dispatchSize);
    }

    @TearDown(Level.Trial)
    public void shutdown() {
        cleanUpDatabase();
        rxSessionFactory.close();
        mutinySessionFactory.close();
        ormSessionFactory.close();
    }

    @TearDown(Level.Iteration)
    public void shutdownExecutors() throws InterruptedException {
        dispatchExecutor.shutdown();
        workerExecutor.shutdown();

        dispatchExecutor.awaitTermination(10, TimeUnit.SECONDS);
        workerExecutor.awaitTermination(10, TimeUnit.SECONDS);
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

        try (Session s = ormSessionFactory.openSession()) {
            s.getTransaction().begin();
            s.persist(author1);
            s.persist(author2);
            s.getTransaction().commit();
        }

        return new Integer[]{author1.getId(), author2.getId()};
    }

    protected void cleanUpDatabase() {
        try (Session s = ormSessionFactory.openSession()) {
            s.getTransaction().begin();
            s.createQuery("delete from Book").executeUpdate();
            s.createQuery("delete from Author").executeUpdate();
            s.getTransaction().commit();
        }
    }

    public ExecutorService getDispatchExecutor() {
        return dispatchExecutor;
    }

    public ExecutorService getWorkerExecutor() {
        return workerExecutor;
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

    public <R> R inOrmSession(Function<Session, R> function) {
        try (Session s = ormSessionFactory.openSession()) {
            return function.apply(s);
        }
    }

    public <R> R inOrmTransaction(Function<Session, R> function) {
        Session s = ormSessionFactory.openSession();
        R value = null;
        try {
            s.getTransaction().begin();
            value = function.apply(s);
            s.getTransaction().commit();
        } catch (Exception e) {
            if (s.getTransaction().isActive()) {
                s.getTransaction().rollback();
            }
        } finally {
            s.close();
        }
        return value;
    }
}
