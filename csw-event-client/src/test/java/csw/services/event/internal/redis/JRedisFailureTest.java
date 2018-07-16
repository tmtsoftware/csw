package csw.services.event.internal.redis;

import akka.NotUsed;
import akka.actor.testkit.typed.javadsl.TestProbe;
import akka.stream.javadsl.Source;
import csw.messages.events.Event;
import csw.services.event.exceptions.PublishFailure;
import csw.services.event.helpers.Utils;
import csw.services.event.javadsl.IEventPublisher;
import io.lettuce.core.ClientOptions;
import io.lettuce.core.RedisException;
import org.junit.*;
import org.junit.rules.ExpectedException;

import java.time.Duration;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.hamcrest.CoreMatchers.isA;

public class JRedisFailureTest {

    private static RedisTestProps redisTestProps;

    @Rule
    public final ExpectedException exception = ExpectedException.none();

    @BeforeClass
    public static void beforeClass() {
        ClientOptions clientOptions = ClientOptions.builder().autoReconnect(false).disconnectedBehavior(ClientOptions.DisconnectedBehavior.REJECT_COMMANDS).build();
        redisTestProps = RedisTestProps.createRedisProperties(4561, 27380, 7380, clientOptions);
        redisTestProps.start();
    }

    @AfterClass
    public static void afterClass() throws Exception {
        redisTestProps.shutdown();
    }

    @Test
    public void failureInPublishingShouldFailFutureWithPublishFailedException() throws InterruptedException, ExecutionException, TimeoutException {
        IEventPublisher publisher = redisTestProps.jEventService().makeNewPublisher().get(10, TimeUnit.SECONDS);
        Event event = Utils.makeEvent(2);
        publisher.publish(event).get(10, TimeUnit.SECONDS);

        publisher.shutdown().get(10, TimeUnit.SECONDS);

        Thread.sleep(1000); // wait till the publisher is shutdown successfully

        exception.expectCause(isA(PublishFailure.class));
        publisher.publish(Utils.makeEvent(2)).get(10, TimeUnit.SECONDS);
    }

    @Test
    public void handleFailedPublishEventWithACallback() throws InterruptedException, ExecutionException, TimeoutException {
        IEventPublisher publisher = redisTestProps.jEventService().makeNewPublisher().get(10, TimeUnit.SECONDS);
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

    @Test
    public void handleFailedPublishEventWithAnEventGeneratorAndACallback() throws InterruptedException, ExecutionException, TimeoutException {
        IEventPublisher publisher = redisTestProps.jEventService().makeNewPublisher().get(10, TimeUnit.SECONDS);
        TestProbe<PublishFailure> testProbe = TestProbe.create(redisTestProps.typedActorSystem());
        publisher.publish(Utils.makeEvent(1)).get(10, TimeUnit.SECONDS);

        publisher.shutdown().get(10, TimeUnit.SECONDS);

        Thread.sleep(1000); // wait till the publisher is shutdown successfully

        Event event = Utils.makeEvent(1);

        publisher.publish(() -> event, Duration.ofMillis(20), failure -> testProbe.ref().tell(failure));

        PublishFailure failure = testProbe.expectMessageClass(PublishFailure.class);
        Assert.assertEquals(failure.event(), event);
        Assert.assertEquals(failure.getCause().getClass(), RedisException.class);
    }
}
