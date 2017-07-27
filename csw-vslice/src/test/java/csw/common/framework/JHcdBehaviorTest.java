package csw.common.framework;

import akka.typed.ActorRef;
import akka.typed.ActorSystem;
import akka.typed.Behavior;
import akka.typed.Props;
import akka.typed.scaladsl.Actor;
import akka.typed.testkit.TestKitSettings;
import akka.typed.testkit.scaladsl.TestProbe;
import akka.util.Timeout;
import csw.common.components.hcd.messages.JHcdDomainMessages;
import csw.common.framework.javadsl.commons.JClassTag;
import csw.common.framework.javadsl.hcd.JHcdHandlers;
import csw.common.framework.javadsl.hcd.JHcdHandlersFactory;
import csw.common.framework.models.HcdMsg;
import csw.common.framework.models.HcdResponseMode;
import csw.common.framework.models.InitialHcdMsg;
import org.junit.Assert;
import org.junit.Ignore;
import scala.concurrent.Await;
import scala.concurrent.Future;
import scala.concurrent.duration.Duration;
import scala.concurrent.duration.FiniteDuration;
import scala.reflect.ClassTag;
import scala.runtime.Nothing$;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static org.mockito.Mockito.*;


public class JHcdBehaviorTest {

    @Ignore
    public void testHcdActorInitialization() throws Exception {
        JHcdHandlers hcdHandlers = mock(JHcdHandlers.class);
        JHcdHandlersFactory<JHcdDomainMessages> hcdHandlerFactory = new JHcdHandlersFactory<JHcdDomainMessages>(JHcdDomainMessages.class) {

            @Override
            public JHcdHandlers<JHcdDomainMessages> make(akka.typed.javadsl.ActorContext<HcdMsg> ctx) {
                return hcdHandlers;
            }
        };

        ActorSystem<Object> actorSystem = ActorSystem.create("actorSystem", Actor.empty());

        TestKitSettings testKitSettings = TestKitSettings.apply(actorSystem);

        TestProbe<HcdResponseMode> supervisorProbe = new TestProbe<HcdResponseMode>(
                "supervisor-probe",
                actorSystem,
                testKitSettings);

        when(hcdHandlers.jInitialize()).thenReturn(CompletableFuture.completedFuture(null));
        doNothing().when(hcdHandlers).onRun();

        Behavior<Nothing$> hcdMsgBehavior = hcdHandlerFactory.behaviour(supervisorProbe.ref()).narrow();

        Future<ActorRef<Nothing$>> hcdMsgActorRefF = actorSystem.systemActorOf(hcdMsgBehavior, "ddd", Props.empty(), Timeout.apply(5, TimeUnit.SECONDS));
        ActorRef<Nothing$> hcdMsgActorRef = Await.result(hcdMsgActorRefF, Duration.create(5, TimeUnit.SECONDS));

        ClassTag<HcdResponseMode.Initialized> initializedClassTag = JClassTag.make(HcdResponseMode.Initialized.class);
        HcdResponseMode.Initialized initialized = supervisorProbe.expectMsgType(FiniteDuration.apply(5, TimeUnit.SECONDS), initializedClassTag);

        Assert.assertEquals(hcdMsgActorRef, initialized.hcdRef());

        initialized.hcdRef().tell(InitialHcdMsg.Run$.MODULE$);

        ClassTag<HcdResponseMode.Running> runningClassTag = JClassTag.make(HcdResponseMode.Running.class);
        HcdResponseMode.Running running = supervisorProbe.expectMsgType(FiniteDuration.apply(5, TimeUnit.SECONDS), runningClassTag);

        Assert.assertEquals(hcdMsgActorRef, running.hcdRef());
    }
}
