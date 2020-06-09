package org.hibernate.reactive.benchmark;

import org.openjdk.jmh.infra.Blackhole;

/**
 * The main class for the reactive benchmarks
 */
public interface ReactiveBenchmark {

    void executeFind(ReactiveBenchmarkState state, Blackhole bh);
}
