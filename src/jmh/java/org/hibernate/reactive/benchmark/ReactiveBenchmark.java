package org.hibernate.reactive.benchmark;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.reactive.model.Author;
import org.hibernate.reactive.model.Book;
import org.hibernate.reactive.stage.Stage;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.TearDown;
import org.openjdk.jmh.infra.Blackhole;
import org.openjdk.jmh.results.RunResult;
import org.openjdk.jmh.results.format.ResultFormatFactory;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;
import org.openjdk.jmh.runner.options.TimeValue;

import java.util.Collection;
import java.util.Properties;
import java.util.concurrent.CompletionStage;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;

import static java.util.concurrent.TimeUnit.MICROSECONDS;
import static javax.persistence.Persistence.createEntityManagerFactory;
import static org.hibernate.cfg.AvailableSettings.JPA_PERSISTENCE_PROVIDER;
import static org.openjdk.jmh.annotations.Mode.AverageTime;
import static org.openjdk.jmh.annotations.Mode.Throughput;
import static org.openjdk.jmh.results.format.ResultFormatType.TEXT;
import static org.openjdk.jmh.runner.options.VerboseMode.NORMAL;

/**
 * The main class for the reactive benchmarks
 */
@BenchmarkMode(AverageTime)
@OutputTimeUnit(MICROSECONDS)
public class ReactiveBenchmark {

    @State(Scope.Benchmark)
    public static class ReactiveBenchmarkState {

        private Stage.SessionFactory rxSessionFactory;
        private SessionFactory sessionFactory;
        private Integer[] ids;

        @Setup
        public void setup() {
            // obtain a factory for reactive sessions based on the standard JPA configuration properties specified in resources/META-INF/persistence.xml
            Properties properties = new Properties();
            properties.setProperty(JPA_PERSISTENCE_PROVIDER, "org.hibernate.reactive.provider.ReactivePersistenceProvider");
            rxSessionFactory = createEntityManagerFactory("example", properties).unwrap(Stage.SessionFactory.class);

            sessionFactory = createEntityManagerFactory("example").unwrap(SessionFactory.class);

            ids = populateDatabase();
        }

        @TearDown
        public void shutdown() {
            cleanUpDatabase();
            rxSessionFactory.close();
            sessionFactory.close();
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

            inTransaction(
                    entityManager -> {
                        entityManager.persist( author1 );
                        entityManager.persist( author2 );
                    }
            );

            return new Integer[] {author1.getId(), author2.getId()};
        }

        protected void cleanUpDatabase() {
            inTransaction(entityManager -> entityManager.createQuery( "delete from Book" ).executeUpdate());
            inTransaction(entityManager -> entityManager.createQuery( "delete from Author" ).executeUpdate());
        }

        // --- //

        public <T> CompletionStage<T> withReactiveSession(Function<Stage.Session, CompletionStage<T>> work) {
            return rxSessionFactory.withSession(work);
        }

        public <T> CompletionStage<T> withReactiveTransaction(BiFunction<Stage.Session, Stage.Transaction, CompletionStage<T>> work) {
            return rxSessionFactory.withTransaction(work);
        }

        public void inSession(Consumer<Session> consumer) {
            try (Session s = sessionFactory.openSession()) {
                consumer.accept(s);
            }
        }

        public void inTransaction(Consumer<Session> consumer) {
            Session s = sessionFactory.openSession();
            try {
                s.getTransaction().begin();
                consumer.accept(s);
                s.getTransaction().commit();
            }
            catch (Exception e) {
                if (s.getTransaction().isActive()) {
                    s.getTransaction().rollback();
                }
            }
            finally {
                s.close();
            }
        }
    }

    // --- //

    @Benchmark
    public void executeFind(ReactiveBenchmarkState state, Blackhole bh) {
        state.inSession(session -> bh.consume(session.find(Author.class, state.ids[0]).getName()));
    }

    @Benchmark
    public void executeReactiveFind(ReactiveBenchmarkState state, Blackhole bh) {
        state.withReactiveSession(session ->
                session.find(Author.class, state.ids[0]).thenAccept(author -> bh.consume(author.getName()))
        ).toCompletableFuture().join();
    }

}
