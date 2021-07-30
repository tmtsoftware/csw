package csw.event.client.internal.redis;

import akka.NotUsed;
import akka.actor.testkit.typed.javadsl.TestProbe;
import akka.stream.javadsl.Source;
import csw.event.api.exceptions.PublishFailure;
import csw.event.api.javadsl.IEventPublisher;
import csw.event.client.helpers.Utils;
import csw.params.events.Event;
import csw.time.core.models.TMTTime;
import csw.time.core.models.UTCTime;
import io.lettuce.core.ClientOptions;
import io.lettuce.core.RedisException;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.scalatestplus.junit.JUnitSuite;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Supplier;

//DEOPSCSW-398: Propagate failure for publish api (eventGenerator)
public class JRedisFailureTest extends JUnitSuite {

    private static RedisTestProps redisTestProps;

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
    public void failureInPublishingShouldFailFutureWithPublishFailedException__DEOPSCSW_398() throws InterruptedException, ExecutionException, TimeoutException {
        IEventPublisher publisher = redisTestProps.jEventService().makeNewPublisher();
        Event event = Utils.makeEvent(2);
        publisher.publish(event).get(10, TimeUnit.SECONDS);

        publisher.shutdown().get(10, TimeUnit.SECONDS);

        Thread.sleep(1000); // wait till the publisher is shutdown successfully

        ExecutionException ex = Assert.assertThrows(ExecutionException.class, () -> publisher.publish(Utils.makeEvent(2)).get(10, TimeUnit.SECONDS));
        Assert.assertTrue(ex.getCause() instanceof PublishFailure);
    }

    //DEOPSCSW-334: Publish an event
    @Test
    public void handleFailedPublishEventWithACallback__DEOPSCSW_398_DEOPSCSW_334() throws InterruptedException, ExecutionException, TimeoutException {
        IEventPublisher publisher = redisTestProps.jEventService().makeNewPublisher();
        TestProbe<PublishFailure> testProbe = TestProbe.create(redisTestProps.actorSystem());
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
    public void handleFailedPublishEventWithAnEventGeneratorAndACallback__DEOPSCSW_398_DEOPSCSW_334() throws InterruptedException, ExecutionException, TimeoutException {
        IEventPublisher publisher = redisTestProps.jEventService().makeNewPublisher();
        TestProbe<PublishFailure> testProbe = TestProbe.create(redisTestProps.actorSystem());
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
    public void handleFailedPublishEventWithAnEventGeneratorGeneratingFutureOfEventAndACallback__DEOPSCSW_398_DEOPSCSW_000() throws InterruptedException, ExecutionException, TimeoutException {
        IEventPublisher publisher = redisTestProps.jEventService().makeNewPublisher();
        TestProbe<PublishFailure> testProbe = TestProbe.create(redisTestProps.actorSystem());
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
    public void handleFailedPublishEventWithAnEventGeneratorGeneratingEventAtSpecificTimeAndACallback__DEOPSCSW_398_DEOPSCSW_515() throws InterruptedException, ExecutionException, TimeoutException {
        IEventPublisher publisher = redisTestProps.jEventService().makeNewPublisher();
        TestProbe<PublishFailure> testProbe = TestProbe.create(redisTestProps.actorSystem());
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
    public void handleFailedPublishEventWithAnEventGeneratorGeneratingFutureOfEventAtSpecificTimeAndACallback__DEOPSCSW_398_DEOPSCSW_515() throws InterruptedException, ExecutionException, TimeoutException {
        IEventPublisher publisher = redisTestProps.jEventService().makeNewPublisher();
        TestProbe<PublishFailure> testProbe = TestProbe.create(redisTestProps.actorSystem());
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
    public void shouldNotInvokeOnErrorOnOptingToNotPublishEventWithEventGenerator__DEOPSCSW_398_DEOPSCSW_516() throws InterruptedException, ExecutionException, TimeoutException {
        IEventPublisher publisher = redisTestProps.jEventService().makeNewPublisher();
        TestProbe<PublishFailure> testProbe = TestProbe.create(redisTestProps.actorSystem());
        publisher.publish(Utils.makeEvent(1)).get(10, TimeUnit.SECONDS);

        publisher.shutdown().get(10, TimeUnit.SECONDS);

        Thread.sleep(1000); // wait till the publisher is shutdown successfully

        publisher.publish(Optional::empty, Duration.ofMillis(20), failure -> testProbe.ref().tell(failure));
        testProbe.expectNoMessage();

        TMTTime startTime = new UTCTime(UTCTime.now().value().plusMillis(200));
        publisher.publish(Optional::empty, startTime, Duration.ofMillis(20), failure -> testProbe.ref().tell(failure));
        testProbe.expectNoMessage(Duration.of(500, ChronoUnit.MILLIS));
    }

    //DEOPSCSW-516: Optionally Publish - API Change
    @Test
    public void shouldNotInvokeOnErrorOnOptingToNotPublishEventWithAsyncEventGenerator__DEOPSCSW_398_DEOPSCSW_516() throws InterruptedException, ExecutionException, TimeoutException {
        IEventPublisher publisher = redisTestProps.jEventService().makeNewPublisher();
        TestProbe<PublishFailure> testProbe = TestProbe.create(redisTestProps.actorSystem());
        publisher.publish(Utils.makeEvent(1)).get(10, TimeUnit.SECONDS);

        publisher.shutdown().get(10, TimeUnit.SECONDS);

        Thread.sleep(1000); // wait till the publisher is shutdown successfully

        Supplier<CompletableFuture<Optional<Event>>> eventGenerator = () -> CompletableFuture.completedFuture(Optional.empty());

        publisher.publishAsync(eventGenerator, Duration.ofMillis(20), failure -> testProbe.ref().tell(failure));
        testProbe.expectNoMessage();

        TMTTime startTime = new UTCTime(UTCTime.now().value().plusMillis(200));
        publisher.publishAsync(eventGenerator, startTime, Duration.ofMillis(20), failure -> testProbe.ref().tell(failure));
        testProbe.expectNoMessage(Duration.of(500, ChronoUnit.MILLIS));
    }
}
