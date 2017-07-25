package csw.common.framework;

import akka.typed.ActorRef;
import akka.typed.ActorSystem;
import akka.typed.Behavior;
import akka.typed.Props;
import akka.typed.javadsl.Actor;
import akka.typed.testkit.TestKitSettings;
import akka.typed.testkit.scaladsl.TestProbe;
import akka.util.Timeout;
import csw.common.components.hcd.JSampleHcdFactory;
import csw.common.components.hcd.messages.HcdSampleMessages;
import csw.common.framework.javadsl.JClassTag;
import csw.common.framework.models.HcdResponseMode;
import csw.common.framework.models.InitialHcdMsg;
import org.junit.Assert;
import org.junit.Test;
import scala.concurrent.Await;
import scala.concurrent.Future;
import scala.concurrent.duration.Duration;
import scala.concurrent.duration.FiniteDuration;
import scala.reflect.ClassTag;
import scala.runtime.Nothing$;

import java.util.concurrent.TimeUnit;

public class JHcdActorTest {

    @Test
    public void testHcdActorInitialization() throws Exception {
        ActorSystem<Object> actorSystem = ActorSystem.create("actorSystem", Actor.empty());

        TestKitSettings testKitSettings = TestKitSettings.apply(actorSystem);

        TestProbe<HcdResponseMode> supervisorProbe = new TestProbe<HcdResponseMode>(
                "supervisor-probe",
                actorSystem,
                testKitSettings);


        Behavior<Nothing$> hcdMsgBehavior = new JSampleHcdFactory(HcdSampleMessages.class).behaviour(supervisorProbe.ref()).narrow();

        Future<ActorRef<Nothing$>> hcdMsgActorRefF = actorSystem.systemActorOf(hcdMsgBehavior, "ddd", Props.empty(), Timeout.apply(5, TimeUnit.SECONDS));
        ActorRef<Nothing$> hcdMsgActorRef = Await.result(hcdMsgActorRefF, Duration.create(5, TimeUnit.SECONDS));

        ClassTag<HcdResponseMode.Initialized> initializedClassTag = JClassTag.make(HcdResponseMode.Initialized.class);
        HcdResponseMode.Initialized initialized = supervisorProbe.expectMsgType(FiniteDuration.apply(5, TimeUnit.SECONDS), initializedClassTag);

        Assert.assertEquals(hcdMsgActorRef, initialized.hcdRef());

        ActorRef<HcdResponseMode.Running> replyTo = supervisorProbe.ref().narrow();
        initialized.hcdRef().tell(new InitialHcdMsg.Run(replyTo));


        ClassTag<HcdResponseMode.Running> runningClassTag = JClassTag.make(HcdResponseMode.Running.class);
        HcdResponseMode.Running running = supervisorProbe.expectMsgType(FiniteDuration.apply(5, TimeUnit.SECONDS), runningClassTag);

        Assert.assertEquals(hcdMsgActorRef, running.hcdRef());
    }
}
