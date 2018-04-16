package csw.services.event.internal.kafka;

import akka.actor.ActorSystem;
import csw.messages.commons.CoordinatedShutdownReasons;
import csw.services.event.helpers.RegistrationFactory;
import csw.services.event.internal.JEventServicePubSubTestFramework;
import csw.services.event.internal.commons.EmbeddedKafkaWiring$;
import csw.services.event.internal.commons.EventServiceConnection;
import csw.services.event.internal.commons.Wiring;
import csw.services.event.javadsl.IEventPublisher;
import csw.services.event.javadsl.IEventSubscriber;
import csw.services.event.javadsl.JKafkaFactory;
import csw.services.location.commons.ClusterAwareSettings;
import csw.services.location.commons.ClusterSettings;
import csw.services.location.models.TcpRegistration;
import csw.services.location.scaladsl.LocationService;
import csw.services.location.scaladsl.LocationServiceFactory;
import net.manub.embeddedkafka.EmbeddedKafka$;
import net.manub.embeddedkafka.EmbeddedKafkaConfig;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import scala.concurrent.Await;
import scala.concurrent.duration.FiniteDuration;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

//DEOPSCSW-334: Publish an event
//DEOPSCSW-335: Model for EventName that encapsulates the topic(or channel ) name
//DEOPSCSW-337: Subscribe to an event based on prefix
public class JKafkaPubSubTest {
    private static int seedPort = 3563;
    private static int kafkaPort = 6001;

    private static IEventPublisher publisher;
    private static Wiring wiring;

    private static JEventServicePubSubTestFramework framework;

    @BeforeClass
    public static void beforeClass() throws Exception {

        ClusterSettings clusterSettings = ClusterAwareSettings.joinLocal(seedPort);

        LocationService locationService = LocationServiceFactory.withSettings(ClusterAwareSettings.onPort(seedPort));
        TcpRegistration tcpRegistration = RegistrationFactory.tcp(EventServiceConnection.value(), kafkaPort);
        Await.result(locationService.register(tcpRegistration), new FiniteDuration(10, TimeUnit.SECONDS));

        ActorSystem actorSystem = clusterSettings.system();

        EmbeddedKafkaConfig config = EmbeddedKafkaWiring$.MODULE$.embeddedKafkaConfig(clusterSettings);

        wiring = new Wiring(actorSystem);
        JKafkaFactory kafkaFactory = new JKafkaFactory(locationService, wiring);
        publisher = kafkaFactory.publisher().get(10, TimeUnit.SECONDS);
        IEventSubscriber subscriber = kafkaFactory.subscriber().get(10, TimeUnit.SECONDS);

        framework = new JEventServicePubSubTestFramework(publisher, subscriber, actorSystem, wiring.resumingMat());

        EmbeddedKafka$.MODULE$.start(config);
    }

    @AfterClass
    public static void afterClass() throws Exception {
        publisher.shutdown().get(10, TimeUnit.SECONDS);
        EmbeddedKafka$.MODULE$.stop();
        Await.result(wiring.shutdown(CoordinatedShutdownReasons.TestFinishedReason$.MODULE$), new FiniteDuration(10, TimeUnit.SECONDS));
    }

    @Test
    public void shouldBeAbleToPublishAndSubscribeAnEvent() throws InterruptedException, TimeoutException, ExecutionException {
        framework.pubsub();
    }

    @Test
    public void shouldAbleToMakeIndependentSubscriptions() throws InterruptedException, ExecutionException, TimeoutException {
        framework.subscribeIndependently();
    }

    @Test
    public void shouldBeAbleToPublishConcurrentlyToTheSameChannel() throws InterruptedException {
        framework.publishMultiple();
    }

    @Test
    public void shouldBeAbleToPublishMultipleToDifferentChannels() throws InterruptedException {
        framework.publishMultipleToDifferentChannels();
    }

    //DEOPSCSW-340: Provide most recently published event for subscribed prefix and name
    @Test
    public void shouldBeAbleToRetrieveRecentlyPublishedEventOnSubscription() throws InterruptedException, ExecutionException, TimeoutException {
        framework.retrieveRecentlyPublished();
    }

    //DEOPSCSW-340: Provide most recently published event for subscribed prefix and name
    @Test
    public void shouldBeAbleToRetrieveInvalidEvent() throws InterruptedException, TimeoutException, ExecutionException {
        framework.retrieveInvalidEvent();
    }

    //DEOPSCSW-340: Provide most recently published event for subscribed prefix and name
    @Test
    public void shouldBeAbleToRetrieveOnlyValidEventsWhenOneOfTheSubscribedEventsKeysHasPublishedEvents() throws InterruptedException, ExecutionException, TimeoutException {
        framework.retrieveMultipleSubscribedEvents();
    }

    //DEOPSCSW-338: Provide callback for Event alerts
    @Test
    public void shouldBeAbleToSubscribeWithCallback() throws InterruptedException, ExecutionException, TimeoutException {
        framework.retrieveEventUsingCallback();
    }

    //DEOPSCSW-338: Provide callback for Event alerts
    @Test
    public void shouldBeAbleToSubscribeWithAsyncCallback() throws InterruptedException, ExecutionException, TimeoutException {
        framework.retrieveEventUsingAsyncCallback();
    }

    //DEOPSCSW-339: Provide actor ref to alert about Event arrival
    @Test
    public void shouldBeAbleToSubscribeWithAnActorRef() throws InterruptedException, ExecutionException, TimeoutException {
        framework.retrieveEventUsingActorRef();
    }

    //DEOPSCSW-344: Retrieve recently published event using prefix and eventname
    @Test
    public void shouldBeAbleToGetAnEventWithoutSubscribingForIt() throws InterruptedException, ExecutionException, TimeoutException {
        framework.get();
    }

    //DEOPSCSW-344: Retrieve recently published event using prefix and eventname
    @Test
    public void shouldBeAbleToRetrieveInvalidEventOnGet() throws InterruptedException, ExecutionException, TimeoutException {
        framework.retrieveInvalidEventOnGet();
    }

    //DEOPSCSW-344: Retrieve recently published event using prefix and eventname
    @Test
    public void shouldBeAbleToRetrieveEventsForMultipleEventKeysOnGet() throws InterruptedException, ExecutionException, TimeoutException {
        framework.retrieveEventsForMultipleEventKeysOnGet();
    }
}
