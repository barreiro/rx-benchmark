package org.hibernate.reactive.benchmark;

import org.hibernate.reactive.model.Author;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.infra.Blackhole;

import java.util.concurrent.ExecutionException;

public class MutinyDispatch implements ReactiveBenchmark {

    @Benchmark
    public void executeFind(ReactiveBenchmarkState state, Blackhole bh) {
        try {
            state.getDispatchExecutor().submit(() ->
                    state.withMutinySession(s -> s.find(Author.class, state.getSingleId())).onItem().invoke(author -> bh.consume(author.getName()) )
            ).get().await().indefinitely();
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException(e);
        }
    }
}
