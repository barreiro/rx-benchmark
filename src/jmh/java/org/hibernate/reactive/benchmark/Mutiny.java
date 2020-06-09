package org.hibernate.reactive.benchmark;

import org.hibernate.reactive.model.Author;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.infra.Blackhole;

public class Mutiny implements ReactiveBenchmark {

    @Benchmark
    public void executeFind(ReactiveBenchmarkState state, Blackhole bh) {
        state.withMutinySession(
                s -> s.find(Author.class, state.getSingleId()).onItem().invoke(
                        author -> bh.consume(author.getName()))
        ).await().indefinitely();
    }
}
