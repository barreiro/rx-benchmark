package org.hibernate.reactive.benchmark;

import org.hibernate.reactive.model.Author;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.infra.Blackhole;

import java.util.concurrent.ExecutionException;

public class OrmDispatch implements ReactiveBenchmark {

    @Benchmark
    public void executeFind(ReactiveBenchmarkState state, Blackhole bh) {
        try {
            state.getDispatchExecutor().submit(() -> {
                try {
                    Author author = state.getWorkerExecutor().submit(() ->
                            state.inOrmSession(session -> session.find(Author.class, state.getSingleId()))
                    ).get();
                    bh.consume(author.getName());
                } catch (InterruptedException | ExecutionException e) {
                    throw new RuntimeException(e);
                }
            }).get();
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException(e);
        }
    }

}
