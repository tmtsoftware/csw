package csw.common.framework.javadsl;

import akka.typed.ActorRef;
import akka.typed.ActorSystem;
import akka.typed.Props;
import akka.typed.javadsl.ActorContext;
import akka.typed.scaladsl.Actor;
import akka.typed.testkit.TestKitSettings;
import akka.util.Timeout;
import csw.SupervisorMsg;
import csw.common.components.hcd.HcdDomainMsg;
import csw.common.framework.models.ComponentInfo;
import csw.common.framework.models.ComponentMsg;
import csw.common.framework.models.LocationServiceUsages;
import csw.common.framework.models.PubSub;
import csw.common.framework.scaladsl.SupervisorBehaviorFactory;
import csw.param.states.CurrentState;
import csw.services.location.javadsl.JConnectionType;
import csw.services.location.models.ConnectionType;
import org.junit.AfterClass;
import scala.concurrent.Await;
import scala.concurrent.Future;
import scala.concurrent.duration.Duration;
import scala.concurrent.duration.FiniteDuration;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;

public class JComponentWiringTest {
    private static ActorSystem system = ActorSystem.create(Actor.empty(), "component-wiring");
    private static TestKitSettings settings = TestKitSettings.apply(system);

    private JComponentWiring getSampleJHcdFactory(JComponentHandlers hcdHandlers) {
        return new JComponentWiring<HcdDomainMsg>(HcdDomainMsg.class) {
            @Override
            public JComponentHandlers<HcdDomainMsg> make(ActorContext<ComponentMsg> ctx, ComponentInfo componentInfo, ActorRef<PubSub.PublisherMsg<CurrentState>> pubSubRef) {
                return hcdHandlers;
            }
        };
    }

    private Set<ConnectionType> connectionType = new HashSet(Arrays.asList(JConnectionType.AkkaType));
    private FiniteDuration rate = new FiniteDuration(5, TimeUnit.SECONDS);

    private ComponentInfo.HcdInfo hcdInfo = JHcdInfoFactory.make("trombone",
            "wfos",
            "csw.common.framework.javadsl.JSampleHcdWiring",
            LocationServiceUsages.JDoNotRegister(),
            connectionType,
            rate);


    @AfterClass
    public static void afterAll() throws Exception {
        Await.result(system.terminate(), Duration.create(5, "seconds"));
    }

    @org.scalatest.Ignore
    public void testComponentWiring() throws Exception {

        Timeout timeout = Timeout.durationToTimeout(FiniteDuration.apply(5, "seconds"));

        Future<ActorRef<SupervisorMsg>> supervisorHcd = system.<SupervisorMsg>systemActorOf(
                SupervisorBehaviorFactory.make(hcdInfo),
                "sampleHcd",
                Props.empty(),
                timeout);

        ActorRef<SupervisorMsg> supervisor = Await.result(
                supervisorHcd,
                FiniteDuration.apply(5,"seconds")
        );
    }
}
