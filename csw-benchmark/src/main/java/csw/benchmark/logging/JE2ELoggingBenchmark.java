/*
 * Copyright (c) 2022 Thirty Meter Telescope International Observatory
 * SPDX-License-Identifier: Apache-2.0
 */

package csw.benchmark.logging;

import akka.actor.typed.ActorSystem;
import akka.actor.typed.SpawnProtocol;
import csw.logging.api.javadsl.ILogger;
import csw.logging.models.Level;
import csw.logging.client.internal.LoggingSystem;
import csw.logging.client.javadsl.JGenericLoggerFactory;
import csw.logging.client.javadsl.JLogAppenderBuilders;
import csw.logging.client.javadsl.JLoggingSystemFactory;
import org.openjdk.jmh.annotations.*;
import scala.concurrent.Await;
import scala.concurrent.duration.Duration;

import java.util.List;
import java.util.concurrent.TimeUnit;


/**
 * Tests CSW E2E java logging performance.
 */
// ============================== HOW TO RUN THIS TEST: ====================================
//
// single thread:
// sbt csw-benchmark/jmh:run -f 1 -wi 10 -i 20 .*JE2ELoggingBenchmark.*
//
// multiple threads (for example, 4 threads):
// sbt csw-benchmark/jmh:run -f 1 -wi 10 -i 20 -t 4 -si true .*JE2ELoggingBenchmark.*
//

// DEOPSCSW-279: Test logging performance
@State(Scope.Benchmark)
@Warmup(iterations = 5)
@Measurement(iterations = 10)
@Fork(value = 1)
public class JE2ELoggingBenchmark {
    private ILogger log;
    private static ActorSystem<SpawnProtocol.Command> actorSystem;
    private JPerson person;

    @Setup
    public void setup() {
        log = JGenericLoggerFactory.getLogger(getClass());
        actorSystem = akka.actor.typed.ActorSystem.create(SpawnProtocol.create(), "JE2E");
        LoggingSystem loggingSystem = JLoggingSystemFactory.start("JE2E-Bench", "SNAPSHOT-1.0", "localhost", actorSystem, List.of(JLogAppenderBuilders.FileAppender()));
        loggingSystem.setDefaultLogLevel(Level.INFO$.MODULE$);
        person = JPerson.createDummy();
    }

    @TearDown
    public void teardown() throws Exception {
        actorSystem.terminate();
        Await.result(actorSystem.whenTerminated(), Duration.create(5, TimeUnit.SECONDS));
    }

    /**
     * All the benchmarks in this file exercises end to end java logging
     * <p>
     * `Setup` : 1. Getting logger instance
     * 2. Starting LoggingSystem with FileAppender
     * 3. Setting default log level to INFO
     * 4. Creating instance of Person class just for logging purpose
     * <p>
     * `TearDown` : 1. Terminating ActorSystem
     * <p>
     * There are two benchmarks to show the difference between supplier and string version of logger API
     * when log level is not enabled (TRACE)
     * 1. logWithSupplierWhenLogLevelIsNotEnabledThroughput (throughput : 58546426.378 ± 1725886.415  ops/s)
     * 2. logWithStringWhenLogLevelIsNotEnabledThroughput (throughput : 4914848.777 ±  314402.782  ops/s)
     * <p>
     * Other two tests are to exercise logging with supplier and String when log level is enabled (INFO)
     * 1. logWithSupplierWhenLogLevelIsEnabledThroughput
     * 2. logWithStringWhenLogLevelIsEnabledThroughput
     */

    @Benchmark
    @BenchmarkMode(Mode.Throughput)
    @OutputTimeUnit(TimeUnit.SECONDS)
    public void logWithSupplierWhenLogLevelIsEnabledThroughput() {
        log.info(() -> "My name is " + person + ", logging with Supplier@INFO");
    }

    @Benchmark
    @BenchmarkMode(Mode.Throughput)
    @OutputTimeUnit(TimeUnit.SECONDS)
    public void logWithSupplierWhenLogLevelIsNotEnabledThroughput() {
        log.trace(() -> "My name is " + person + ", logging with Supplier@TRACE");
    }

    @Benchmark
    @BenchmarkMode(Mode.Throughput)
    @OutputTimeUnit(TimeUnit.SECONDS)
    public void logWithStringWhenLogLevelIsEnabledThroughput() {
        log.info("My name is " + person + ", logging with String@INFO");
    }

    @Benchmark
    @BenchmarkMode(Mode.Throughput)
    @OutputTimeUnit(TimeUnit.SECONDS)
    public void logWithStringWhenLogLevelIsNotEnabledThroughput() {
        log.trace("My name is " + person + ", logging with String@INFO");
    }
}

class JPerson {
    private final String firstName;
    private final String lastName;
    private final String address;
    private final String city;

    public JPerson(String firstName, String lastName, String address, String city) {
        this.firstName = firstName;
        this.lastName = lastName;
        this.address = address;
        this.city = city;
    }

    static JPerson createDummy() {
        return new JPerson("James", "Bond", "Downtown", "Omaha");
    }

    @Override
    public String toString() {
        return "Person{" +
                "firstName='" + firstName + '\'' +
                ", lastName='" + lastName + '\'' +
                ", address='" + address + '\'' +
                ", city='" + city + '\'' +
                '}';
    }
}
