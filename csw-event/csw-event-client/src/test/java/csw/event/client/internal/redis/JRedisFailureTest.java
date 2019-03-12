package csw.event.client.internal.redis;

import akka.NotUsed;
import akka.actor.testkit.typed.javadsl.TestProbe;
import akka.stream.javadsl.Source;
import csw.params.events.Event;
import csw.event.api.exceptions.PublishFailure;
import csw.event.api.javadsl.IEventPublisher;
import csw.event.client.helpers.Utils;
import csw.time.core.models.TMTTime;
import csw.time.core.models.UTCTime;
import io.lettuce.core.ClientOptions;
import io.lettuce.core.RedisException;
import org.junit.*;
import org.junit.rules.ExpectedException;
import org.scalatestplus.junit.JUnitSuite;

import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.hamcrest.CoreMatchers.isA;

//DEOPSCSW-398: Propagate failure for publish api (eventGenerator)
public class JRedisFailureTest extends JUnitSuite {

    private static RedisTestProps redisTestProps;

    @Rule
    public final ExpectedException exception = ExpectedException.none();

    @BeforeClass
    public static void beforeClass() {
        ClientOptions clientOptions = ClientOptions.builder().autoReconnect(false).disconnectedBehavior(ClientOptions.DisconnectedBehavior.REJECT_COMMANDS).build();
        redisTestProps = RedisTestProps.jCreateRedisProperties(clientOptions);
        redisTestProps.start();
    }

    @AfterClass
    public static void afterClass() {
        redisTestProps.shutdown();
    }

    @Test
    public void failureInPublishingShouldFailFutureWithPublishFailedException() throws InterruptedException, ExecutionException, TimeoutException {
        IEventPublisher publisher = redisTestProps.jEventService().makeNewPublisher();
        Event event = Utils.makeEvent(2);
        publisher.publish(event).get(10, TimeUnit.SECONDS);

        publisher.shutdown().get(10, TimeUnit.SECONDS);

        Thread.sleep(1000); // wait till the publisher is shutdown successfully

        exception.expectCause(isA(PublishFailure.class));
        publisher.publish(Utils.makeEvent(2)).get(10, TimeUnit.SECONDS);
    }

    //DEOPSCSW-334: Publish an event
    @Test
    public void handleFailedPublishEventWithACallback() throws InterruptedException, ExecutionException, TimeoutException {
        IEventPublisher publisher = redisTestProps.jEventService().makeNewPublisher();
        TestProbe<PublishFailure> testProbe = TestProbe.create(redisTestProps.typedActorSystem());
        publisher.publish(Utils.makeEvent(1)).get(10, TimeUnit.SECONDS);

        publisher.shutdown().get(10, TimeUnit.SECONDS);

        Thread.sleep(1000); // wait till the publisher is shutdown successfully

        Event event = Utils.makeEvent(1);
        Source<Event, NotUsed> eventStream = Source.single(event);

        publisher.publish(eventStream, failure -> testProbe.ref().tell(failure));

        PublishFailure failure = testProbe.expectMessageClass(PublishFailure.class);
        Assert.assertEquals(failure.event(), event);
        Assert.assertEquals(failure.getCause().getClass(), RedisException.class);
    }

    //DEOPSCSW-334: Publish an event
    @Test
    public void handleFailedPublishEventWithAnEventGeneratorAndACallback() throws InterruptedException, ExecutionException, TimeoutException {
        IEventPublisher publisher = redisTestProps.jEventService().makeNewPublisher();
        TestProbe<PublishFailure> testProbe = TestProbe.create(redisTestProps.typedActorSystem());
        publisher.publish(Utils.makeEvent(1)).get(10, TimeUnit.SECONDS);

        publisher.shutdown().get(10, TimeUnit.SECONDS);

        Thread.sleep(1000); // wait till the publisher is shutdown successfully

        Event event = Utils.makeEvent(1);

        publisher.publish(() -> Optional.ofNullable(event), Duration.ofMillis(20), failure -> testProbe.ref().tell(failure));

        PublishFailure failure = testProbe.expectMessageClass(PublishFailure.class);
        Assert.assertEquals(failure.event(), event);
        Assert.assertEquals(failure.getCause().getClass(), RedisException.class);
    }

    //DEOPSCSW-000: Publish events with block generating futre of event
    @Test
    public void handleFailedPublishEventWithAnEventGeneratorGeneratingFutureOfEventAndACallback() throws InterruptedException, ExecutionException, TimeoutException {
        IEventPublisher publisher = redisTestProps.jEventService().makeNewPublisher();
        TestProbe<PublishFailure> testProbe = TestProbe.create(redisTestProps.typedActorSystem());
        publisher.publish(Utils.makeEvent(1)).get(10, TimeUnit.SECONDS);

        publisher.shutdown().get(10, TimeUnit.SECONDS);

        Thread.sleep(1000); // wait till the publisher is shutdown successfully

        Event event = Utils.makeEvent(1);

        publisher.publishAsync(() -> CompletableFuture.completedFuture(Optional.ofNullable(event)), Duration.ofMillis(20), failure -> testProbe.ref().tell(failure));

        PublishFailure failure = testProbe.expectMessageClass(PublishFailure.class);
        Assert.assertEquals(failure.event(), event);
        Assert.assertEquals(failure.getCause().getClass(), RedisException.class);
    }

    //DEOPSCSW-515: Include Start Time in API
    @Test
    public void handleFailedPublishEventWithAnEventGeneratorGeneratingEventAtSpecificTimeAndACallback() throws InterruptedException, ExecutionException, TimeoutException {
        IEventPublisher publisher = redisTestProps.jEventService().makeNewPublisher();
        TestProbe<PublishFailure> testProbe = TestProbe.create(redisTestProps.typedActorSystem());
        publisher.publish(Utils.makeEvent(1)).get(10, TimeUnit.SECONDS);

        publisher.shutdown().get(10, TimeUnit.SECONDS);

        Thread.sleep(1000); // wait till the publisher is shutdown successfully

        Event event = Utils.makeEvent(1);

        TMTTime startTime = new UTCTime(UTCTime.now().value().plusMillis(500));

        publisher.publish(() -> Optional.ofNullable(event), startTime, Duration.ofMillis(20), failure -> testProbe.ref().tell(failure));

        PublishFailure failure = testProbe.expectMessageClass(PublishFailure.class);
        Assert.assertEquals(failure.event(), event);
        Assert.assertEquals(failure.getCause().getClass(), RedisException.class);
    }

    //DEOPSCSW-515: Include Start Time in API
    @Test
    public void handleFailedPublishEventWithAnEventGeneratorGeneratingFutureOfEventAtSpecificTimeAndACallback() throws InterruptedException, ExecutionException, TimeoutException {
        IEventPublisher publisher = redisTestProps.jEventService().makeNewPublisher();
        TestProbe<PublishFailure> testProbe = TestProbe.create(redisTestProps.typedActorSystem());
        publisher.publish(Utils.makeEvent(1)).get(10, TimeUnit.SECONDS);

        publisher.shutdown().get(10, TimeUnit.SECONDS);

        Thread.sleep(1000); // wait till the publisher is shutdown successfully

        Event event = Utils.makeEvent(1);

        TMTTime startTime = new UTCTime(UTCTime.now().value().plusMillis(500));

        publisher.publishAsync(() -> CompletableFuture.completedFuture(Optional.ofNullable(event)), startTime, Duration.ofMillis(20), failure -> testProbe.ref().tell(failure));

        PublishFailure failure = testProbe.expectMessageClass(PublishFailure.class);
        Assert.assertEquals(failure.event(), event);
        Assert.assertEquals(failure.getCause().getClass(), RedisException.class);
    }

    //DEOPSCSW-516: Optionally Publish - API Change
    @Test
    public void handleEmptyPublishEventWithAnEventGeneratorGeneratingEventAtSpecificTimeAndACallback() throws InterruptedException, ExecutionException, TimeoutException {
        IEventPublisher publisher = redisTestProps.jEventService().makeNewPublisher();
        TestProbe<PublishFailure> testProbe = TestProbe.create(redisTestProps.typedActorSystem());
        publisher.publish(Utils.makeEvent(1)).get(10, TimeUnit.SECONDS);

        publisher.shutdown().get(10, TimeUnit.SECONDS);

        Thread.sleep(1000); // wait till the publisher is shutdown successfully

        TMTTime startTime = new UTCTime(UTCTime.now().value().plusMillis(500));

        publisher.publish(Optional::empty, startTime, Duration.ofMillis(20), failure -> testProbe.ref().tell(failure));

        testProbe.expectNoMessage();
    }

    //DEOPSCSW-516: Optionally Publish - API Change
    @Test
    public void handleEmptyPublishEventWithAnEventGeneratorGeneratingFutureOfEventAtSpecificTimeAndACallback() throws InterruptedException, ExecutionException, TimeoutException {
        IEventPublisher publisher = redisTestProps.jEventService().makeNewPublisher();
        TestProbe<PublishFailure> testProbe = TestProbe.create(redisTestProps.typedActorSystem());
        publisher.publish(Utils.makeEvent(1)).get(10, TimeUnit.SECONDS);

        publisher.shutdown().get(10, TimeUnit.SECONDS);

        Thread.sleep(1000); // wait till the publisher is shutdown successfully

        TMTTime startTime = new UTCTime(UTCTime.now().value().plusMillis(500));

        publisher.publishAsync(() -> CompletableFuture.completedFuture(Optional.empty()), startTime, Duration.ofMillis(20), failure -> testProbe.ref().tell(failure));

        testProbe.expectNoMessage();
    }
}
