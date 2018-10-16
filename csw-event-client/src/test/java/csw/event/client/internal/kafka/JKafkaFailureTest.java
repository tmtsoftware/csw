package csw.event.client.internal.kafka;

import akka.actor.testkit.typed.javadsl.TestProbe;
import akka.actor.typed.javadsl.Adapter;
import akka.stream.javadsl.Source;
import csw.params.events.Event;
import csw.event.api.exceptions.PublishFailure;
import csw.event.api.javadsl.IEventPublisher;
import csw.event.client.helpers.Utils;
import net.manub.embeddedkafka.EmbeddedKafka$;
import org.apache.kafka.common.errors.RecordTooLargeException;
import org.junit.*;
import org.junit.rules.ExpectedException;

import java.time.Duration;
import java.util.Collections;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.hamcrest.CoreMatchers.isA;

public class JKafkaFailureTest {

    private static KafkaTestProps kafkaTestProps;
    private static IEventPublisher publisher;

    @Rule
    public final ExpectedException exception = ExpectedException.none();

    @BeforeClass
    public static void beforeClass() {
        kafkaTestProps = KafkaTestProps.jCreateKafkaProperties(Collections.singletonMap("message.max.bytes", "1"));
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

    @Test
    public void handleFailedPublishEventWithAnEventGeneratorAndACallback() {
        TestProbe<PublishFailure> testProbe = TestProbe.create(Adapter.toTyped(kafkaTestProps.actorSystem()));
        Event event = Utils.makeEvent(1);

        publisher.publish(() -> event, Duration.ofMillis(20), failure -> testProbe.ref().tell(failure));

        PublishFailure failure = testProbe.expectMessageClass(PublishFailure.class);
        Assert.assertEquals(failure.event(), event);
        Assert.assertEquals(failure.getCause().getClass(), RecordTooLargeException.class);
    }

    @Test
    public void handleFailedPublishEventWithAnEventGeneratorGeneratingFutureOfEventAndACallback() {
        TestProbe<PublishFailure> testProbe = TestProbe.create(Adapter.toTyped(kafkaTestProps.actorSystem()));
        Event event = Utils.makeEvent(1);

        publisher.publishAsync(() -> CompletableFuture.completedFuture(event), Duration.ofMillis(20), failure -> testProbe.ref().tell(failure));

        PublishFailure failure = testProbe.expectMessageClass(PublishFailure.class);
        Assert.assertEquals(failure.event(), event);
        Assert.assertEquals(failure.getCause().getClass(), RecordTooLargeException.class);
    }
}
