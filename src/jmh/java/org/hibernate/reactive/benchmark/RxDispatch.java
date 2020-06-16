package org.hibernate.reactive.benchmark;

import org.hibernate.reactive.model.Author;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.infra.Blackhole;

import java.util.concurrent.ExecutionException;

public class RxDispatch implements ReactiveBenchmark {

    @Benchmark
    public void executeFind(ReactiveBenchmarkState state, Blackhole bh) {
        try {
            state.getDispatchExecutor().submit(() ->
                    state.withReactiveSession(session -> session.find(Author.class, state.getSingleId())).thenAccept(author -> bh.consume(author.getName()))
            ).get().toCompletableFuture().join();
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException(e);
        }
    }
}
