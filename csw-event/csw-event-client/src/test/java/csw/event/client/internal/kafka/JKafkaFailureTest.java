package csw.event.client.internal.kafka;

import akka.actor.testkit.typed.javadsl.TestProbe;
import akka.actor.typed.javadsl.Adapter;
import akka.stream.javadsl.Source;
import csw.event.api.exceptions.PublishFailure;
import csw.event.api.javadsl.IEventPublisher;
import csw.event.client.helpers.Utils;
import csw.params.events.Event;
import csw.time.core.models.TMTTime;
import csw.time.core.models.UTCTime;
import net.manub.embeddedkafka.EmbeddedKafka$;
import org.apache.kafka.common.errors.RecordTooLargeException;
import org.junit.*;
import org.junit.rules.ExpectedException;
import org.scalatestplus.junit.JUnitSuite;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Supplier;

import static org.hamcrest.CoreMatchers.isA;

//DEOPSCSW-398: Propagate failure for publish api (eventGenerator)
public class JKafkaFailureTest extends JUnitSuite {

    private static KafkaTestProps kafkaTestProps;
    private static IEventPublisher publisher;

    @Rule
    public final ExpectedException exception = ExpectedException.none();

    @BeforeClass
    public static void beforeClass() {
        kafkaTestProps = KafkaTestProps.jCreateKafkaProperties(Map.of("message.max.bytes", "1"));
        publisher = kafkaTestProps.jPublisher();
        EmbeddedKafka$.MODULE$.start(kafkaTestProps.config());
    }

    @AfterClass
    public static void afterClass() {
        kafkaTestProps.shutdown();
    }

    @Test
    public void failureInPublishingShouldFailFutureWithPublishFailedException() throws InterruptedException, ExecutionException, TimeoutException {
        exception.expectCause(isA(PublishFailure.class));

        // simulate publishing failure as message size is greater than message.max.bytes(1 byte) configured in broker
        publisher.publish(Utils.makeEvent(2)).get(10, TimeUnit.SECONDS);
    }

    //DEOPSCSW-334: Publish an event
    @Test
    public void handleFailedPublishEventWithACallback() {

        TestProbe<PublishFailure> testProbe = TestProbe.create(Adapter.toTyped(kafkaTestProps.actorSystem()));
        Event event = Utils.makeEvent(1);
        Event eventSent = event;
        Source eventStream = Source.single(eventSent);

        publisher.publish(eventStream, failure -> testProbe.ref().tell(failure));

        PublishFailure failure = testProbe.expectMessageClass(PublishFailure.class);
        Assert.assertEquals(failure.event(), event);
        Assert.assertEquals(failure.getCause().getClass(), RecordTooLargeException.class);
    }

    //DEOPSCSW-334: Publish an event
    @Test
    public void handleFailedPublishEventWithAnEventGeneratorAndACallback() {
        TestProbe<PublishFailure> testProbe = TestProbe.create(Adapter.toTyped(kafkaTestProps.actorSystem()));
        Event event = Utils.makeEvent(1);

        publisher.publish(() -> Optional.ofNullable(event), Duration.ofMillis(20), failure -> testProbe.ref().tell(failure));

        PublishFailure failure = testProbe.expectMessageClass(PublishFailure.class);
        Assert.assertEquals(failure.event(), event);
        Assert.assertEquals(failure.getCause().getClass(), RecordTooLargeException.class);
    }

    //DEOPSCSW-000: Publish events with block generating futre of event
    @Test
    public void handleFailedPublishEventWithAnEventGeneratorGeneratingFutureOfEventAndACallback() {
        TestProbe<PublishFailure> testProbe = TestProbe.create(Adapter.toTyped(kafkaTestProps.actorSystem()));
        Event event = Utils.makeEvent(1);

        publisher.publishAsync(() -> CompletableFuture.completedFuture(Optional.ofNullable(event)), Duration.ofMillis(20), failure -> testProbe.ref().tell(failure));

        PublishFailure failure = testProbe.expectMessageClass(PublishFailure.class);
        Assert.assertEquals(failure.event(), event);
        Assert.assertEquals(failure.getCause().getClass(), RecordTooLargeException.class);
    }

    //DEOPSCSW-515: Include Start Time in API
    @Test
    public void handleFailedPublishEventWithAnEventGeneratorGeneratingEventAtSpecificStartTimeAndACallback() {
        TestProbe<PublishFailure> testProbe = TestProbe.create(Adapter.toTyped(kafkaTestProps.actorSystem()));
        Event event = Utils.makeEvent(1);

        TMTTime startTime = new UTCTime(UTCTime.now().value().plusMillis(500));

        publisher.publish(() -> Optional.ofNullable(event),startTime, Duration.ofMillis(20), failure -> testProbe.ref().tell(failure));

        PublishFailure failure = testProbe.expectMessageClass(PublishFailure.class);
        Assert.assertEquals(failure.event(), event);
        Assert.assertEquals(failure.getCause().getClass(), RecordTooLargeException.class);
    }

    //DEOPSCSW-515: Include Start Time in API
    @Test
    public void handleFailedPublishEventWithAnEventGeneratorGeneratingFutureOfEventAtSpecificStartTimeAndACallback() {
        TestProbe<PublishFailure> testProbe = TestProbe.create(Adapter.toTyped(kafkaTestProps.actorSystem()));
        Event event = Utils.makeEvent(1);

        TMTTime startTime = new UTCTime(UTCTime.now().value().plusMillis(500));

        publisher.publishAsync(() -> CompletableFuture.completedFuture(Optional.ofNullable(event)), startTime, Duration.ofMillis(20), failure -> testProbe.ref().tell(failure));

        PublishFailure failure = testProbe.expectMessageClass(PublishFailure.class);
        Assert.assertEquals(failure.event(), event);
        Assert.assertEquals(failure.getCause().getClass(), RecordTooLargeException.class);
    }

    //DEOPSCSW-516: Optionally Publish - API Change
    @Test
    public void shouldNotInvokeOnErrorOnOptingToNotPublishEventWithEventGenerator() {
        TestProbe<PublishFailure> testProbe = TestProbe.create(Adapter.toTyped(kafkaTestProps.actorSystem()));

        publisher.publish(Optional::empty, Duration.ofMillis(20), failure -> testProbe.ref().tell(failure));
        testProbe.expectNoMessage();

        TMTTime startTime = new UTCTime(UTCTime.now().value().plusMillis(200));
        publisher.publish(Optional::empty, startTime, Duration.ofMillis(20), failure -> testProbe.ref().tell(failure));
        testProbe.expectNoMessage(Duration.of(500, ChronoUnit.MILLIS));
    }

    //DEOPSCSW-516: Optionally Publish - API Change
    @Test
    public void shouldNotInvokeOnErrorOnOptingToNotPublishEventWithAsyncEventGenerator() {
        TestProbe<PublishFailure> testProbe = TestProbe.create(Adapter.toTyped(kafkaTestProps.actorSystem()));
        Supplier<CompletableFuture<Optional<Event>>> eventGenerator = () -> CompletableFuture.completedFuture(Optional.empty());

        publisher.publishAsync(eventGenerator, Duration.ofMillis(20), failure -> testProbe.ref().tell(failure));
        testProbe.expectNoMessage();

        TMTTime startTime = new UTCTime(UTCTime.now().value().plusMillis(200));
        publisher.publishAsync(eventGenerator, startTime, Duration.ofMillis(20), failure -> testProbe.ref().tell(failure));
        testProbe.expectNoMessage(Duration.of(500, ChronoUnit.MILLIS));
    }

}
