package org.hibernate.reactive.benchmark;

import org.hibernate.reactive.model.Author;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.infra.Blackhole;

import java.util.concurrent.CompletableFuture;

public class OrmDispatch implements ReactiveBenchmark {

    @Benchmark
    public void executeFind(ReactiveBenchmarkState state, Blackhole bh) {
        CompletableFuture.runAsync(() ->
                        state.inOrmSession(session -> bh.consume(session.find(Author.class, state.getSingleId()).getName()))
                , state.getExecutor()).join();
    }

}
