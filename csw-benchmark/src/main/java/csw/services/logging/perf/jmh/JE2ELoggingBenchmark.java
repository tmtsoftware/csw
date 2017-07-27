package csw.services.logging.perf.jmh;

import akka.actor.ActorSystem;
import csw.services.logging.internal.LoggingLevels;
import csw.services.logging.internal.LoggingSystem;
import csw.services.logging.javadsl.ILogger;
import csw.services.logging.javadsl.JGenericLogger;
import csw.services.logging.javadsl.JLogAppenderBuilders;
import csw.services.logging.javadsl.JLoggingSystemFactory;
import org.openjdk.jmh.annotations.*;
import scala.concurrent.Await;
import scala.concurrent.duration.Duration;

import java.util.Collections;
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
public class JE2ELoggingBenchmark implements JGenericLogger {
    private ILogger log;
    private static ActorSystem actorSystem;
    private JPerson person;

    @Setup
    public void setup() {
        log = getLogger();
        actorSystem = ActorSystem.create("JE2E");
        LoggingSystem loggingSystem = JLoggingSystemFactory.start("JE2E-Bench", "SNAPSHOT-1.0", "localhost", actorSystem, Collections.singletonList(JLogAppenderBuilders.FileAppender));
        loggingSystem.setDefaultLogLevel(LoggingLevels.INFO$.MODULE$);
        person = JPerson.createDummy();
    }

    @TearDown
    public void teardown() throws Exception {
        Await.result(actorSystem.terminate(), Duration.create(5, TimeUnit.SECONDS));
    }

    /**
    * All the benchmarks in this file exercises end to end java logging
    *
    * `Setup` : 1. Getting logger instance
    *          2. Starting LoggingSystem with FileAppender
    *          3. Setting default log level to INFO
    *          4. Creating instance of Person class just for logging purpose
    *
    * `TearDown` : 1. Terminating ActorSystem
    *
    * There are two benchmarks to show the difference between supplier and string version of logger API
    * when log level is not enabled (TRACE)
    *   1. logWithSupplierWhenLogLevelIsNotEnabledThroughput (throughput : 58546426.378 ± 1725886.415  ops/s)
    *   2. logWithStringWhenLogLevelIsNotEnabledThroughput (throughput : 4914848.777 ±  314402.782  ops/s)
    *
    * Other two tests are to exercise logging with supplier and String when log level is enabled (INFO)
    *   1. logWithSupplierWhenLogLevelIsEnabledThroughput
    *   2. logWithStringWhenLogLevelIsEnabledThroughput
    *
    * */

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
    private String firstName;
    private String lastName;
    private String address;
    private String city;

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
