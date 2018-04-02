package csw.services.event.redis.internal;

import akka.actor.ActorSystem;
import akka.japi.Pair;
import akka.stream.javadsl.Keep;
import akka.stream.javadsl.Sink;
import csw.messages.commons.CoordinatedShutdownReasons;
import csw.messages.events.Event;
import csw.messages.events.Event$;
import csw.messages.events.EventKey;
import csw.services.event.JRedisFactory;
import csw.services.event.helpers.RegistrationFactory;
import csw.services.event.helpers.Utils;
import csw.services.event.internal.commons.EventServiceConnection;
import csw.services.event.internal.commons.Wiring;
import csw.services.event.javadsl.IEventPublisher;
import csw.services.event.javadsl.IEventSubscriber;
import csw.services.event.javadsl.IEventSubscription;
import csw.services.location.commons.ClusterAwareSettings;
import csw.services.location.commons.ClusterSettings;
import csw.services.location.models.TcpRegistration;
import csw.services.location.scaladsl.LocationService;
import csw.services.location.scaladsl.LocationServiceFactory;
import io.lettuce.core.RedisClient;
import org.junit.*;
import redis.embedded.RedisServer;
import scala.concurrent.Await;
import scala.concurrent.duration.FiniteDuration;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class PubSubTest {
    private static int seedPort = 3562;
    private static int redisPort = 6379;

    private static ClusterSettings clusterSettings = ClusterAwareSettings.joinLocal(seedPort);

    private static RedisServer redis = RedisServer.builder().setting("bind " + clusterSettings.hostname()).port(redisPort).build();
    private static RedisClient redisClient;
    private static Wiring wiring;
    private IEventPublisher publisher;
    private IEventSubscriber subscriber;

    public PubSubTest() throws Exception {
        TcpRegistration tcpRegistration = RegistrationFactory.tcp(EventServiceConnection.value(), redisPort);
        LocationService locationService = LocationServiceFactory.withSettings(ClusterAwareSettings.onPort(seedPort));
        Await.result(locationService.register(tcpRegistration), new FiniteDuration(10, TimeUnit.SECONDS));
        redisClient = RedisClient.create();
        ActorSystem actorSystem = clusterSettings.system();
        wiring = new Wiring(actorSystem);
        JRedisFactory redisFactory = new JRedisFactory(redisClient, locationService, wiring);
        publisher = redisFactory.publisher().get();
        subscriber = redisFactory.subscriber().get();
    }

    @BeforeClass
    public static void beforeClass() {
        redis.start();
    }

    @AfterClass
    public static void afterClass() throws Exception {
        redisClient.shutdown();
        redis.stop();
        Await.result(wiring.shutdown(CoordinatedShutdownReasons.TestFinishedReason$.MODULE$), new FiniteDuration(10, TimeUnit.SECONDS));
    }

    @Test
    public void shouldBeAbleToPublishAndSubscribeAnEvent() throws InterruptedException, TimeoutException, ExecutionException {
        Event event1 = Utils.makeDistinctEvent(1);
        EventKey eventKey = event1.eventKey();

        java.util.Set<EventKey> set = new HashSet<>();
        set.add(eventKey);

        Pair<IEventSubscription, CompletionStage<List<Event>>> subscriptionFuturePair = subscriber.subscribe(set).toMat(Sink.seq(), Keep.both()).run(wiring.resumingMat());
        Thread.sleep(2000);

        publisher.publish(event1);
        Thread.sleep(1000);

        subscriptionFuturePair.first().unsubscribe().get(10, TimeUnit.SECONDS);

        List<Event> expectedEvents = new ArrayList<>();
        expectedEvents.add(Event$.MODULE$.invalidEvent());
        expectedEvents.add(event1);
        Assert.assertArrayEquals(subscriptionFuturePair.second().toCompletableFuture().get().toArray(), expectedEvents.toArray());
    }
}
