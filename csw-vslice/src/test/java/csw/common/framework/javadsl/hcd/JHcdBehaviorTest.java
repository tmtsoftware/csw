package csw.common.framework.javadsl.hcd;

import akka.typed.ActorRef;
import akka.typed.ActorSystem;
import akka.typed.Behavior;
import akka.typed.Props;
import akka.typed.javadsl.ActorContext;
import akka.typed.scaladsl.Actor;
import akka.typed.testkit.TestKitSettings;
import akka.typed.testkit.scaladsl.TestProbe;
import akka.util.Timeout;
import csw.common.components.hcd.HcdDomainMsg;
import csw.common.framework.javadsl.JComponentWiring;
import csw.common.framework.javadsl.JComponentHandlers;
import csw.common.framework.javadsl.commons.JClassTag;
import csw.common.framework.models.*;
import csw.param.states.CurrentState;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;
import scala.concurrent.Await;
import scala.concurrent.Future;
import scala.concurrent.duration.Duration;
import scala.concurrent.duration.FiniteDuration;
import scala.runtime.BoxedUnit;
import scala.runtime.Nothing$;

import java.util.concurrent.CompletableFuture;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class JHcdBehaviorTest {
    private static ActorSystem system = ActorSystem.create(Actor.empty(), "Hcd");
    private static TestKitSettings settings = TestKitSettings.apply(system);

    private JComponentWiring getSampleJHcdFactory(JComponentHandlers hcdHandlers) {
        return new JComponentWiring<HcdDomainMsg>(HcdDomainMsg.class) {
            @Override
            public JComponentHandlers<HcdDomainMsg> make(ActorContext<ComponentMsg> ctx, ComponentInfo componentInfo, ActorRef<PubSub.PublisherMsg<CurrentState>> pubSubRef) {
                return hcdHandlers;
            }
        };
    }

    private ComponentInfo.HcdInfo hcdInfo = new ComponentInfo.HcdInfo("trombone",
            "wfos",
            "csw.common.components.hcd.SampleHcd",
            LocationServiceUsages.JDoNotRegister(),
            null,
            null);


    @AfterClass
    public static void afterAll() throws Exception {
        Await.result(system.terminate(), Duration.create(5, "seconds"));
    }

    @Test
    public void testHcdBehavior() throws Exception {

        JComponentHandlers<HcdDomainMsg> sampleHcdHandler = Mockito.mock(JComponentHandlers.class);
        when(sampleHcdHandler.initialize()).thenCallRealMethod();
        when(sampleHcdHandler.jInitialize()).thenReturn(CompletableFuture.completedFuture(
                BoxedUnit.UNIT
        ));

        TestProbe<FromComponentLifecycleMessage> supervisorProbe = TestProbe.apply(system, settings);

        Timeout seconds = Timeout.durationToTimeout(FiniteDuration.apply(5, "seconds"));
        Behavior<Nothing$> behavior = getSampleJHcdFactory(sampleHcdHandler).compBehavior(hcdInfo, supervisorProbe.ref(), null);
        Future<ActorRef> hcd = system.<Nothing$>systemActorOf(behavior, "hcd", Props.empty(), seconds);
        FiniteDuration seconds1 = Duration.create(5, "seconds");
        ActorRef hcdRef = Await.result(hcd, seconds1);

        SupervisorIdleMsg.Initialized initialized = supervisorProbe.expectMsgType(JClassTag.make(SupervisorIdleMsg.Initialized.class));
        Assert.assertEquals(hcdRef, initialized.componentRef());

        initialized.componentRef().tell(InitialMsg.Run$.MODULE$);

        SupervisorIdleMsg.Running running = supervisorProbe.expectMsgType(JClassTag.make(SupervisorIdleMsg.Running.class));
        verify(sampleHcdHandler).onRun();
        verify(sampleHcdHandler).isOnline_$eq(true);

        Assert.assertEquals(hcdRef, running.componentRef());
    }
}
