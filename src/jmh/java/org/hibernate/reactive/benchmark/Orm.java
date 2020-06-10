package org.hibernate.reactive.benchmark;

import org.hibernate.reactive.model.Author;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.infra.Blackhole;

public class Orm implements ReactiveBenchmark {

    @Benchmark
    public void executeFind(ReactiveBenchmarkState state, Blackhole bh) {
        Author author = state.inOrmSession(session -> session.find(Author.class, state.getSingleId()));
        bh.consume(author.getName());
    }

}
