package csw.services.event.internal.redis;

import akka.NotUsed;
import akka.actor.ActorSystem;
import akka.actor.typed.javadsl.Adapter;
import akka.stream.javadsl.Source;
import akka.testkit.typed.javadsl.TestProbe;
import csw.messages.commons.CoordinatedShutdownReasons;
import csw.messages.events.Event;
import csw.services.event.exceptions.PublishFailed;
import csw.services.event.helpers.RegistrationFactory;
import csw.services.event.helpers.Utils;
import csw.services.event.internal.JEventServicePubSubTestFramework;
import csw.services.event.internal.commons.EventServiceConnection;
import csw.services.event.internal.commons.FailedEvent;
import csw.services.event.internal.commons.Wiring;
import csw.services.event.javadsl.IEventPublisher;
import csw.services.event.javadsl.IEventSubscriber;
import csw.services.event.javadsl.JRedisFactory;
import csw.services.location.commons.ClusterAwareSettings;
import csw.services.location.commons.ClusterSettings;
import csw.services.location.models.TcpRegistration;
import csw.services.location.scaladsl.LocationService;
import csw.services.location.scaladsl.LocationServiceFactory;
import io.lettuce.core.ClientOptions;
import io.lettuce.core.RedisClient;
import org.junit.*;
import org.junit.rules.ExpectedException;
import redis.embedded.RedisServer;
import scala.concurrent.Await;
import scala.concurrent.duration.FiniteDuration;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.hamcrest.CoreMatchers.isA;

public class JRedisFailureTest {
    private static int seedPort = 3565;
    private static int redisPort = 6379;

    private static ClusterSettings clusterSettings;
    private static RedisServer redis;
    private static RedisClient redisClient;
    private static Wiring wiring;
    private static IEventPublisher publisher;
    private static JEventServicePubSubTestFramework framework;
    private static JRedisFactory redisFactory;
    private static ActorSystem actorSystem;
    @Rule
    public final ExpectedException exception = ExpectedException.none();

    @BeforeClass
    public static void beforeClass() throws Exception {
        clusterSettings = ClusterAwareSettings.joinLocal(seedPort);
        redis = RedisServer.builder().setting("bind " + clusterSettings.hostname()).port(redisPort).build();

        TcpRegistration tcpRegistration = RegistrationFactory.tcp(EventServiceConnection.value(), redisPort);
        LocationService locationService = LocationServiceFactory.withSettings(ClusterAwareSettings.onPort(seedPort));
        Await.result(locationService.register(tcpRegistration), new FiniteDuration(10, TimeUnit.SECONDS));

        redisClient = RedisClient.create();
        redisClient.setOptions(
                ClientOptions.builder().autoReconnect(false).disconnectedBehavior(ClientOptions.DisconnectedBehavior.REJECT_COMMANDS).build()
        );
        actorSystem = clusterSettings.system();
        wiring = new Wiring(actorSystem);
        redisFactory = new JRedisFactory(redisClient, locationService, wiring);

        IEventSubscriber subscriber = redisFactory.subscriber().get();

        framework = new JEventServicePubSubTestFramework(publisher, subscriber, actorSystem, wiring.resumingMat());

        redis.start();
    }

    @AfterClass
    public static void afterClass() throws Exception {
        redisClient.shutdown();
        redis.stop();
        Await.result(wiring.shutdown(CoordinatedShutdownReasons.TestFinishedReason$.MODULE$), new FiniteDuration(10, TimeUnit.SECONDS));
    }

    @Test
    public void failureInPublishingShouldFailFutureWithPublishFailedException() throws InterruptedException, ExecutionException, TimeoutException {
        publisher = redisFactory.publisher().get(10, TimeUnit.SECONDS);
        publisher.publish(Utils.makeEvent(2)).get(10, TimeUnit.SECONDS);

        publisher.shutdown().get(10, TimeUnit.SECONDS);

        Thread.sleep(1000); // wait till the publisher is shutdown successfully

        exception.expectCause(isA(PublishFailed.class));
        publisher.publish(Utils.makeEvent(2)).get(10, TimeUnit.SECONDS);
    }

    @Test
    public void handleFailedPublishEventWithACallback() throws InterruptedException, ExecutionException, TimeoutException {
        publisher = redisFactory.publisher().get(10, TimeUnit.SECONDS);

        TestProbe testProbe = TestProbe.create(Adapter.toTyped(actorSystem));
        publisher.publish(Utils.makeEvent(1)).get(10, TimeUnit.SECONDS);

        publisher.shutdown().get(10, TimeUnit.SECONDS);

        Thread.sleep(1000); // wait till the publisher is shutdown successfully

        Event event = Utils.makeEvent(1);
        Source<Event, NotUsed> eventStream = Source.single(event);

        publisher.publish(eventStream, (event1, ex) -> testProbe.ref().tell(new FailedEvent(event1, ex)));

        FailedEvent failedEvent = (FailedEvent) testProbe.expectMessageClass(FailedEvent.class);

        Assert.assertEquals(event, failedEvent.event());
        Assert.assertTrue(failedEvent.throwable() instanceof PublishFailed);
    }

    @Test
    public void handleFailedPublishEventWithAnEventGeneratorAndACallback() throws InterruptedException, ExecutionException, TimeoutException {
        publisher = redisFactory.publisher().get(10, TimeUnit.SECONDS);

        TestProbe testProbe = TestProbe.create(Adapter.toTyped(actorSystem));
        publisher.publish(Utils.makeEvent(1)).get(10, TimeUnit.SECONDS);

        publisher.shutdown().get(10, TimeUnit.SECONDS);

        Thread.sleep(1000); // wait till the publisher is shutdown successfully

        Event event = Utils.makeEvent(1);

        publisher.publish(() -> event, new FiniteDuration(20, TimeUnit.MILLISECONDS), (event1, ex) -> testProbe.ref().tell(new FailedEvent(event1, ex)));

        FailedEvent failedEvent = (FailedEvent) testProbe.expectMessageClass(FailedEvent.class);

        Assert.assertEquals(event, failedEvent.event());
        Assert.assertTrue(failedEvent.throwable() instanceof PublishFailed);
    }
}
