package csw.common.framework;

import akka.typed.ActorRef;
import akka.typed.ActorSystem;
import akka.typed.Behavior;
import akka.typed.javadsl.Adapter;
import akka.typed.testkit.TestKitSettings;
import akka.typed.testkit.scaladsl.TestProbe;
import csw.common.components.hcd.JSampleHcd;
import csw.common.components.hcd.JSampleHcdFactory;
import csw.common.components.hcd.messages.HcdSampleMessages;
import csw.common.framework.models.HcdComponentLifecycleMessage;
import csw.common.framework.models.HcdMsg;
import csw.common.framework.models.InitialHcdMsg;
import org.junit.Assert;
import org.junit.Test;
import scala.concurrent.duration.FiniteDuration;
import scala.reflect.ClassTag;
import scala.runtime.Nothing$;

import java.util.concurrent.TimeUnit;

public class JHcdActorTest {

    @Test
    public void testHcdActorInitialization() {
        akka.actor.ActorSystem untypedActorSystem = akka.actor.ActorSystem.create("untypedActorSystem");

        ActorSystem<Void> typedActorSystem = Adapter.toTyped(untypedActorSystem);
        TestKitSettings testKitSettings = TestKitSettings.apply(typedActorSystem);

        TestProbe<HcdComponentLifecycleMessage> supervisorProbe = new TestProbe<HcdComponentLifecycleMessage>(
                "supervisor-probe",
                typedActorSystem,
                testKitSettings);


        Behavior<Nothing$> hcdMsgBehavior = new JSampleHcdFactory(HcdSampleMessages.class).behaviour(supervisorProbe.ref()).narrow();

        ActorRef<Nothing$> hcdMsgActorRef = Adapter.spawn(untypedActorSystem, hcdMsgBehavior, "hcd");

        ClassTag<HcdComponentLifecycleMessage.Initialized> initializedClassTag = scala.reflect.ClassTag$.MODULE$.apply(HcdComponentLifecycleMessage.Initialized.class);
        HcdComponentLifecycleMessage.Initialized initialized = supervisorProbe.expectMsgType(FiniteDuration.apply(5, TimeUnit.SECONDS), initializedClassTag);

        Assert.assertEquals(hcdMsgActorRef, initialized.hcdRef());

        ActorRef<HcdComponentLifecycleMessage.Running> replyTo = supervisorProbe.ref().narrow();
        initialized.hcdRef().tell(new InitialHcdMsg.Run(replyTo));


        ClassTag<HcdComponentLifecycleMessage.Running> runningClassTag = scala.reflect.ClassTag$.MODULE$.apply(HcdComponentLifecycleMessage.Running.class);
        HcdComponentLifecycleMessage.Running running = supervisorProbe.expectMsgType(FiniteDuration.apply(5, TimeUnit.SECONDS), runningClassTag);

        Assert.assertEquals(hcdMsgActorRef, running.hcdRef());
    }
}
