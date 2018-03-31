//package redis;
//
//import akka.actor.ActorSystem;
//import akka.stream.scaladsl.Sink;
//import csw.messages.events.Event;
//import csw.messages.events.EventKey;
//import csw.services.event.JRedisFactory;
//import csw.services.event.RedisFactory;
//import csw.services.event.helpers.Utils;
//import csw.services.event.internal.commons.EventServiceConnection;
//import csw.services.event.internal.commons.Wiring;
//import csw.services.event.javadsl.IEventPublisher;
//import csw.services.event.scaladsl.EventSubscriber;
//import csw.services.location.commons.ClusterAwareSettings;
//import csw.services.location.commons.ClusterSettings;
//import csw.services.location.commons.RegistrationFactory;
//import csw.services.location.models.TcpRegistration;
//import csw.services.location.scaladsl.LocationService;
//import csw.services.location.scaladsl.LocationServiceFactory;
//import io.lettuce.core.RedisClient;
//import org.junit.Test;
//import redis.embedded.RedisServer;
//import scala.collection.immutable.ListSet;
//import scala.collection.immutable.Set;
//import scala.collection.immutable.Set$;
//
//import java.util.concurrent.ExecutionException;
//
//public class RedisPubSubTest {
//    private int seedPort = 3558;
//    private int redisPort = 6379;
//    private ClusterSettings clusterSettings = ClusterAwareSettings.joinLocal(seedPort);
//    private LocationService locationService = LocationServiceFactory.withSettings(ClusterAwareSettings.onPort(seedPort));
//    private TcpRegistration tcpRegistration = RegistrationFactory.tcp(EventServiceConnection.value(), redisPort);
//    private RedisServer redis = RedisServer.builder().setting("bind ${clusterSettings.hostname}").port(redisPort).build();
//    private ActorSystem actorSystem = clusterSettings.system();
//    private RedisClient redisClient = RedisClient.create();
//    private Wiring wiring = new Wiring(actorSystem);
//    private JRedisFactory redisFactory = new JRedisFactory(redisClient, locationService, wiring);
//    private IEventPublisher publisher = redisFactory.publisher().get();
//    private EventSubscriber subscriber = redisFactory.subscriber().get();
//
//    public RedisPubSubTest() throws ExecutionException, InterruptedException {
//        locationService.register(tcpRegistration);
//    }
//    @Test
//    public void shouldBeAbleToPublishAndSubscribeAnEvent() {
//        Event event1      = Utils.makeDistinctEvent(1);
//        EventKey eventKey = event1.eventKey();
//
//        Set<EventKey> set = new ListSet<EventKey>().$plus(eventKey);
//
//        Thread.sleep(2000);
//
//        publisher.publish(event1);
//        Thread.sleep(1000);
//
//        subscription.unsubscribe().await
//        seqF.await shouldBe List(Event.invalidEvent, event1)
//    }
//}
