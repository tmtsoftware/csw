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
import csw.common.components.hcd.HcdDomainMessage;
import csw.common.framework.javadsl.commons.JClassTag;
import csw.common.framework.models.Component;
import csw.common.framework.models.HcdMsg;
import csw.common.framework.models.HcdResponseMode;
import csw.common.framework.models.InitialHcdMsg;
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
        return new JHcdHandlersFactory<HcdDomainMessage>(HcdDomainMessage.class) {
            @Override
            public JHcdHandlers<HcdDomainMessage> make(ActorContext<HcdMsg> ctx, Component.HcdInfo hcdInfo) {
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
        system.terminate();
    }

    @Test
    public void testHcdBehavior() throws Exception {

        JHcdHandlers<HcdDomainMessage> sampleHcdHandler = Mockito.mock(JHcdHandlers.class);
        when(sampleHcdHandler.initialize()).thenCallRealMethod();
        when(sampleHcdHandler.jInitialize()).thenReturn(CompletableFuture.completedFuture(
                BoxedUnit.UNIT
        ));

        TestProbe<HcdResponseMode> supervisorProbe = TestProbe.apply(system, settings);

        Timeout seconds = Timeout.durationToTimeout(FiniteDuration.apply(5, "seconds"));
        Behavior<Nothing$> behavior = getSampleJHcdFactory(sampleHcdHandler).behavior(hcdInfo, supervisorProbe.ref());
        Future<ActorRef> hcd = system.<Nothing$>systemActorOf(behavior, "hcd", Props.empty(), seconds);
        FiniteDuration seconds1 = Duration.create(5, "seconds");
        ActorRef hcdRef = Await.result(hcd, seconds1);

        HcdResponseMode.Initialized initialized = supervisorProbe.expectMsgType(JClassTag.make(HcdResponseMode.Initialized.class));
        Assert.assertEquals(hcdRef, initialized.hcdRef());

        initialized.hcdRef().tell(InitialHcdMsg.Run$.MODULE$);

        HcdResponseMode.Running running = supervisorProbe.expectMsgType(JClassTag.make(HcdResponseMode.Running.class));
        verify(sampleHcdHandler).onRun();
        verify(sampleHcdHandler).isOnline_$eq(true);

        Assert.assertEquals(hcdRef, running.hcdRef());
    }
}
