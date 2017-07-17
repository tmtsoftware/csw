package csw.services.logging.perf.jmh;

import akka.actor.ActorSystem;
import csw.services.logging.javadsl.ILogger;
import csw.services.logging.javadsl.JGenericLogger;
import csw.services.logging.javadsl.JLogAppenderBuilders;
import csw.services.logging.javadsl.JLoggingSystemFactory;
import org.openjdk.jmh.annotations.*;
import scala.concurrent.Await;
import scala.concurrent.duration.Duration;

import java.util.Collections;
import java.util.concurrent.TimeUnit;

@State(Scope.Benchmark)
@Warmup(iterations = 5)
@Measurement(iterations = 10)
@Fork(value = 1)
public class JE2ELoggingBenchmark implements JGenericLogger {
    private ILogger log;
    private static ActorSystem actorSystem;
    private Person person;

    @Setup
    public void setup() {
        log = getLogger();
        actorSystem = ActorSystem.create("JE2E");
        JLoggingSystemFactory.start("JE2E-Bench", "SNAPSHOT-1.0", "localhost", actorSystem, Collections.singletonList(JLogAppenderBuilders.FileAppender));
        person = Person.createDummy();
    }

    @TearDown
    public void teardown() throws Exception {
        Await.result(actorSystem.terminate(), Duration.create(5, TimeUnit.SECONDS));
    }

    @Benchmark
    @BenchmarkMode(Mode.Throughput)
    @OutputTimeUnit(TimeUnit.SECONDS)
    public void logWithSupplierThroughput() {
        log.info(() -> "My name is " + person + ", logging with Supplier@INFO");
    }

    @Benchmark
    @BenchmarkMode(Mode.Throughput)
    @OutputTimeUnit(TimeUnit.SECONDS)
    public void logWithSupplierWhenLevelIsNotEnabledThroughput() {
        log.trace(()-> "My name is " + person + ", logging with Supplier@TRACE");
    }

    @Benchmark
    @BenchmarkMode(Mode.Throughput)
    @OutputTimeUnit(TimeUnit.SECONDS)
    public void logWithStringThroughput() {
        log.info("My name is " + person + ", logging with String@INFO");
    }

    @Benchmark
    @BenchmarkMode(Mode.Throughput)
    @OutputTimeUnit(TimeUnit.SECONDS)
    public void logWithStringWhenLevelIsNotEnabledThroughput() {

        log.trace("My name is " + person + ", logging with String@INFO");
    }
}

class Person {
    private String firstName;
    private String lastName;
    private String address;
    private String city;

    public Person(String firstName, String lastName, String address, String city) {
        this.firstName = firstName;
        this.lastName = lastName;
        this.address = address;
        this.city = city;
    }

    static Person createDummy() {
        return new Person("James", "Bond", "Downtown", "Omaha");
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
