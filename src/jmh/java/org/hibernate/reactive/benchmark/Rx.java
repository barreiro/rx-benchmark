package org.hibernate.reactive.benchmark;

import org.hibernate.reactive.model.Author;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.infra.Blackhole;

public class Rx implements ReactiveBenchmark {

    @Benchmark
    public void executeFind(ReactiveBenchmarkState state, Blackhole bh) {
        Author author = state.withReactiveSession(session -> session.find(Author.class, state.getSingleId())).toCompletableFuture().join();
        bh.consume(author.getName());
    }
}
