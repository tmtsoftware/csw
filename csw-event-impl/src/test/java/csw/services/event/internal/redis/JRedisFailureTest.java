package csw.services.event.internal.redis;

import akka.NotUsed;
import akka.actor.typed.javadsl.Adapter;
import akka.stream.Materializer;
import akka.stream.javadsl.Source;
import akka.testkit.typed.javadsl.TestProbe;
import csw.messages.commons.CoordinatedShutdownReasons;
import csw.messages.events.Event;
import csw.services.event.exceptions.PublishFailedException;
import csw.services.event.helpers.Utils;
import csw.services.event.javadsl.IEventPublisher;
import csw.services.event.javadsl.JRedisFactory;
import io.lettuce.core.ClientOptions;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import scala.concurrent.Await;
import scala.concurrent.ExecutionContext;
import scala.concurrent.duration.FiniteDuration;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.hamcrest.CoreMatchers.isA;

public class JRedisFailureTest {

    private static RedisTestProps redisTestProps;
    private static JRedisFactory jRedisFactory;
    private IEventPublisher publisher;

    @Rule
    public final ExpectedException exception = ExpectedException.none();

    @BeforeClass
    public static void beforeClass() {
        ClientOptions clientOptions = ClientOptions.builder().autoReconnect(false).disconnectedBehavior(ClientOptions.DisconnectedBehavior.REJECT_COMMANDS).build();
        redisTestProps = RedisTestProps.createRedisProperties(4561, 27380, 7380, clientOptions);
        ExecutionContext executionContext = redisTestProps.wiring().ec();
        Materializer materializer = redisTestProps.wiring().resumingMat();
        jRedisFactory = new JRedisFactory(redisTestProps.redisFactory(), executionContext, materializer);
        redisTestProps.redisSentinel().start();
        redisTestProps.redis().start();
    }

    @AfterClass
    public static void afterClass() throws Exception {
        redisTestProps.redisClient().shutdown();
        redisTestProps.redis().stop();
        redisTestProps.redisSentinel().stop();
        Await.result(redisTestProps.wiring().shutdown(CoordinatedShutdownReasons.TestFinishedReason$.MODULE$), new FiniteDuration(10, TimeUnit.SECONDS));
    }

    @Test
    public void failureInPublishingShouldFailFutureWithPublishFailedException() throws InterruptedException, ExecutionException, TimeoutException {
        publisher = jRedisFactory.publisher("mymaster").get(10, TimeUnit.SECONDS);
        publisher.publish(Utils.makeEvent(2)).get(10, TimeUnit.SECONDS);

        publisher.shutdown().get(10, TimeUnit.SECONDS);

        Thread.sleep(1000); // wait till the publisher is shutdown successfully

        exception.expectCause(isA(PublishFailedException.class));
        publisher.publish(Utils.makeEvent(2)).get(10, TimeUnit.SECONDS);
    }

    @Test
    public void handleFailedPublishEventWithACallback() throws InterruptedException, ExecutionException, TimeoutException {
        publisher = jRedisFactory.publisher("mymaster").get(10, TimeUnit.SECONDS);

        TestProbe testProbe = TestProbe.<Event>create(Adapter.toTyped(redisTestProps.wiring().actorSystem()));
        publisher.publish(Utils.makeEvent(1)).get(10, TimeUnit.SECONDS);

        publisher.shutdown().get(10, TimeUnit.SECONDS);

        Thread.sleep(1000); // wait till the publisher is shutdown successfully

        Event event = Utils.makeEvent(1);
        Source<Event, NotUsed> eventStream = Source.single(event);

        publisher.publish(eventStream, event1 -> testProbe.ref().tell(event1));

        testProbe.<Event>expectMessage(event);
    }

    @Test
    public void handleFailedPublishEventWithAnEventGeneratorAndACallback() throws InterruptedException, ExecutionException, TimeoutException {
        publisher = jRedisFactory.publisher("mymaster").get(10, TimeUnit.SECONDS);

        TestProbe testProbe = TestProbe.create(Adapter.toTyped(redisTestProps.wiring().actorSystem()));
        publisher.publish(Utils.makeEvent(1)).get(10, TimeUnit.SECONDS);

        publisher.shutdown().get(10, TimeUnit.SECONDS);

        Thread.sleep(1000); // wait till the publisher is shutdown successfully

        Event event = Utils.makeEvent(1);

        publisher.publish(() -> event, new FiniteDuration(20, TimeUnit.MILLISECONDS), event1 -> testProbe.ref().tell(event1));

        testProbe.<Event>expectMessage(event);
    }
}
