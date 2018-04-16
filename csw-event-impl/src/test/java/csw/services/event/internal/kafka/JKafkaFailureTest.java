package csw.services.event.internal.kafka;

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
import csw.services.event.internal.commons.EmbeddedKafkaWiring$;
import csw.services.event.internal.commons.EventServiceConnection;
import csw.services.event.internal.commons.FailedEvent;
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
import org.junit.*;
import org.junit.rules.ExpectedException;
import scala.concurrent.Await;
import scala.concurrent.duration.FiniteDuration;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.hamcrest.CoreMatchers.isA;

public class JKafkaFailureTest {
    private static int seedPort = 3564;
    private static int kafkaPort = 6001;

    private static IEventPublisher publisher;
    private static Wiring wiring;
    private static JEventServicePubSubTestFramework framework;
    private static ActorSystem actorSystem;

    @Rule
    public final ExpectedException exception = ExpectedException.none();

    @BeforeClass
    public static void beforeClass() throws Exception {

        ClusterSettings clusterSettings = ClusterAwareSettings.joinLocal(seedPort);

        LocationService locationService = LocationServiceFactory.withSettings(ClusterAwareSettings.onPort(seedPort));
        TcpRegistration tcpRegistration = RegistrationFactory.tcp(EventServiceConnection.value(), kafkaPort);
        Await.result(locationService.register(tcpRegistration), new FiniteDuration(10, TimeUnit.SECONDS));

        actorSystem = clusterSettings.system();

        EmbeddedKafkaConfig config = EmbeddedKafkaWiring$.MODULE$.embeddedKafkaConfigForFailure(clusterSettings);

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
    public void failureInPublishingShouldFailFutureWithPublishFailedException() throws InterruptedException, ExecutionException, TimeoutException {
        exception.expectCause(isA(PublishFailed.class));

        // simulate publishing failure as message size is greater than message.max.bytes(1 byte) configured in broker
        publisher.publish(Utils.makeEvent(2)).get(10, TimeUnit.SECONDS);
    }

    @Test
    public void handleFailedPublishEventWithACallback() {

        TestProbe testProbe = TestProbe.create(Adapter.toTyped(actorSystem));
        Event event = Utils.makeEvent(1);
        Source eventStream = Source.single(event);

        publisher.publish(eventStream, (event1, ex) -> testProbe.ref().tell(new FailedEvent(event1, ex)));

        FailedEvent failedEvent = (FailedEvent) testProbe.expectMessageClass(FailedEvent.class);

        Assert.assertEquals(event, failedEvent.event());
        Assert.assertTrue(failedEvent.throwable() instanceof PublishFailed);
    }

    @Test
    public void handleFailedPublishEventWithAnEventGeneratorAndACallback() {
        TestProbe testProbe = TestProbe.create(Adapter.toTyped(actorSystem));
        Event event = Utils.makeEvent(1);

        publisher.publish(() -> event, new FiniteDuration(20, TimeUnit.MILLISECONDS), (event1, ex) -> testProbe.ref().tell(new FailedEvent(event1, ex)));

        FailedEvent failedEvent = (FailedEvent) testProbe.expectMessageClass(FailedEvent.class);

        Assert.assertEquals(event, failedEvent.event());
        Assert.assertTrue(failedEvent.throwable() instanceof PublishFailed);
    }
}
