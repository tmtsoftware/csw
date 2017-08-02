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
import csw.common.framework.javadsl.commons.JClassTag;
import csw.common.framework.models.*;
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

import static csw.common.framework.models.JComponent.DoNotRegister;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class JHcdBehaviorTest {
    private static ActorSystem system = ActorSystem.create("Hcd", Actor.empty());
    private static TestKitSettings settings = TestKitSettings.apply(system);

    private JHcdHandlersFactory getSampleJHcdFactory(JHcdHandlers hcdHandlers) {
        return new JHcdHandlersFactory<HcdDomainMsg>(HcdDomainMsg.class) {
            @Override
            public JHcdHandlers<HcdDomainMsg> make(ActorContext<ComponentMsg> ctx, Component.HcdInfo hcdInfo) {
                return hcdHandlers;
            }
        };
    }

    private Component.HcdInfo hcdInfo = new Component.HcdInfo("trombone",
            "wfos",
            "csw.common.components.hcd.SampleHcd",
            DoNotRegister,
            null,
            null);


    @AfterClass
    public static void afterAll() throws Exception {
        Await.result(system.terminate(), Duration.create(5, "seconds"));
    }

    @Test
    public void testHcdBehavior() throws Exception {

        JHcdHandlers<HcdDomainMsg> sampleHcdHandler = Mockito.mock(JHcdHandlers.class);
        when(sampleHcdHandler.initialize()).thenCallRealMethod();
        when(sampleHcdHandler.jInitialize()).thenReturn(CompletableFuture.completedFuture(
                BoxedUnit.UNIT
        ));

        TestProbe<FromComponentLifecycleMessage> supervisorProbe = TestProbe.apply(system, settings);

        Timeout seconds = Timeout.durationToTimeout(FiniteDuration.apply(5, "seconds"));
        Behavior<Nothing$> behavior = getSampleJHcdFactory(sampleHcdHandler).behavior(hcdInfo, supervisorProbe.ref());
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
