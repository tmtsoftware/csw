package csw.event.client.internal.kafka;

import akka.actor.Cancellable;
import akka.actor.testkit.typed.javadsl.TestProbe;
import akka.stream.javadsl.Source;
import csw.event.api.exceptions.PublishFailure;
import csw.event.api.javadsl.IEventPublisher;
import csw.event.client.helpers.Utils;
import csw.params.events.Event;
import csw.time.core.models.TMTTime;
import csw.time.core.models.UTCTime;
import io.github.embeddedkafka.EmbeddedKafka$;
import org.apache.kafka.common.errors.RecordTooLargeException;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
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

//DEOPSCSW-398: Propagate failure for publish api (eventGenerator)
public class JKafkaFailureTest extends JUnitSuite {

    private static KafkaTestProps kafkaTestProps;
    private static IEventPublisher publisher;

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
    public void failureInPublishingShouldFailFutureWithPublishFailedException__DEOPSCSW_398() throws InterruptedException, ExecutionException, TimeoutException {
        // simulate publishing failure as message size is greater than message.max.bytes(1 byte) configured in broker
        ExecutionException ex = Assert.assertThrows(ExecutionException.class, () -> publisher.publish(Utils.makeEvent(2)).get(10, TimeUnit.SECONDS));
        Assert.assertTrue(ex.getCause() instanceof PublishFailure);
    }

    //DEOPSCSW-334: Publish an event
    @Test
    public void handleFailedPublishEventWithACallback__DEOPSCSW_398_DEOPSCSW_334() {

        TestProbe<PublishFailure> testProbe = TestProbe.create(kafkaTestProps.actorSystem());
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
    public void handleFailedPublishEventWithAnEventGeneratorAndACallback__DEOPSCSW_398_DEOPSCSW_334() {
        TestProbe<PublishFailure> testProbe = TestProbe.create(kafkaTestProps.actorSystem());
        Event event = Utils.makeEvent(1);

        Cancellable cancellable = publisher.publish(() -> Optional.ofNullable(event), Duration.ofMillis(20), failure -> testProbe.ref().tell(failure));

        PublishFailure failure = testProbe.expectMessageClass(PublishFailure.class);
        Assert.assertEquals(failure.event(), event);
        Assert.assertEquals(failure.getCause().getClass(), RecordTooLargeException.class);
        cancellable.cancel();
    }

    //DEOPSCSW-000: Publish events with block generating future of event
    @Test
    public void handleFailedPublishEventWithAnEventGeneratorGeneratingFutureOfEventAndACallback__DEOPSCSW_398_DEOPSCSW_000() {
        TestProbe<PublishFailure> testProbe = TestProbe.create(kafkaTestProps.actorSystem());
        Event event = Utils.makeEvent(1);

        Cancellable cancellable = publisher.publishAsync(() -> CompletableFuture.completedFuture(Optional.ofNullable(event)), Duration.ofMillis(20), failure -> testProbe.ref().tell(failure));

        PublishFailure failure = testProbe.expectMessageClass(PublishFailure.class);
        Assert.assertEquals(failure.event(), event);
        Assert.assertEquals(failure.getCause().getClass(), RecordTooLargeException.class);
        cancellable.cancel();
    }

    //DEOPSCSW-515: Include Start Time in API
    @Test
    public void handleFailedPublishEventWithAnEventGeneratorGeneratingEventAtSpecificStartTimeAndACallback__DEOPSCSW_398_DEOPSCSW_515() {
        TestProbe<PublishFailure> testProbe = TestProbe.create(kafkaTestProps.actorSystem());
        Event event = Utils.makeEvent(1);

        TMTTime startTime = new UTCTime(UTCTime.now().value().plusMillis(500));

        Cancellable cancellable = publisher.publish(() -> Optional.ofNullable(event), startTime, Duration.ofMillis(20), failure -> testProbe.ref().tell(failure));

        PublishFailure failure = testProbe.expectMessageClass(PublishFailure.class);
        Assert.assertEquals(failure.event(), event);
        Assert.assertEquals(failure.getCause().getClass(), RecordTooLargeException.class);
        cancellable.cancel();
    }

    //DEOPSCSW-515: Include Start Time in API
    @Test
    public void handleFailedPublishEventWithAnEventGeneratorGeneratingFutureOfEventAtSpecificStartTimeAndACallback__DEOPSCSW_398_DEOPSCSW_515() {
        TestProbe<PublishFailure> testProbe = TestProbe.create(kafkaTestProps.actorSystem());
        Event event = Utils.makeEvent(1);

        TMTTime startTime = new UTCTime(UTCTime.now().value().plusMillis(500));

        Cancellable cancellable = publisher.publishAsync(() -> CompletableFuture.completedFuture(Optional.ofNullable(event)), startTime, Duration.ofMillis(20), failure -> testProbe.ref().tell(failure));

        PublishFailure failure = testProbe.expectMessageClass(PublishFailure.class);
        Assert.assertEquals(failure.event(), event);
        Assert.assertEquals(failure.getCause().getClass(), RecordTooLargeException.class);
        cancellable.cancel();
    }

    //DEOPSCSW-516: Optionally Publish - API Change
    @Test
    public void shouldNotInvokeOnErrorOnOptingToNotPublishEventWithEventGenerator__DEOPSCSW_398_DEOPSCSW_516() {
        TestProbe<PublishFailure> testProbe = TestProbe.create(kafkaTestProps.actorSystem());

        Cancellable cancellable = publisher.publish(Optional::empty, Duration.ofMillis(20), failure -> testProbe.ref().tell(failure));
        testProbe.expectNoMessage();

        TMTTime startTime = new UTCTime(UTCTime.now().value().plusMillis(200));
        publisher.publish(Optional::empty, startTime, Duration.ofMillis(20), failure -> testProbe.ref().tell(failure));
        testProbe.expectNoMessage(Duration.of(500, ChronoUnit.MILLIS));
        cancellable.cancel();
    }

    //DEOPSCSW-516: Optionally Publish - API Change
    @Test
    public void shouldNotInvokeOnErrorOnOptingToNotPublishEventWithAsyncEventGenerator__DEOPSCSW_398_DEOPSCSW_516() {
        TestProbe<PublishFailure> testProbe = TestProbe.create(kafkaTestProps.actorSystem());
        Supplier<CompletableFuture<Optional<Event>>> eventGenerator = () -> CompletableFuture.completedFuture(Optional.empty());

        Cancellable cancellable1 = publisher.publishAsync(eventGenerator, Duration.ofMillis(20), failure -> testProbe.ref().tell(failure));
        testProbe.expectNoMessage();

        TMTTime startTime = new UTCTime(UTCTime.now().value().plusMillis(200));
        Cancellable cancellable2 = publisher.publishAsync(eventGenerator, startTime, Duration.ofMillis(20), failure -> testProbe.ref().tell(failure));
        testProbe.expectNoMessage(Duration.of(500, ChronoUnit.MILLIS));
        cancellable1.cancel();
        cancellable2.cancel();
    }

}
