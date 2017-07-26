package csw.common.framework;

import akka.typed.ActorRef;
import akka.typed.ActorSystem;
import akka.typed.Behavior;
import akka.typed.Props;
import akka.typed.javadsl.Actor;
import akka.typed.testkit.TestKitSettings;
import akka.typed.testkit.scaladsl.TestProbe;
import akka.util.Timeout;
import csw.common.components.assembly.JSampleAssemblyFactory;
import csw.common.components.assembly.messages.JAssemblyDomainMessages;
import csw.common.framework.javadsl.assembly.JAssemblyInfoFactory;
import csw.common.framework.javadsl.commons.JClassTag;
import csw.common.framework.models.AssemblyComponentLifecycleMessage;
import csw.common.framework.models.AssemblyComponentLifecycleMessage.Initialized;
import csw.common.framework.models.AssemblyComponentLifecycleMessage.Running;
import csw.common.framework.models.Component.AssemblyInfo;
import csw.common.framework.models.InitialAssemblyMsg;
import csw.common.framework.models.JComponent;
import csw.services.location.javadsl.JConnectionType;
import csw.services.location.models.Connection;
import csw.services.location.models.ConnectionType;
import org.junit.Assert;
import org.junit.Test;
import scala.concurrent.Await;
import scala.concurrent.Future;
import scala.concurrent.duration.Duration;
import scala.concurrent.duration.FiniteDuration;
import scala.reflect.ClassTag;
import scala.runtime.Nothing$;

import java.util.Collections;
import java.util.Set;
import java.util.concurrent.TimeUnit;

public class JAssemblyActorTest {
    @Test
    public void testAssemblyActorSendsInitializedAndRunningMessageToSupervisor() throws Exception {
        ActorSystem<Nothing$> actorSystem = ActorSystem.create("actorSystem", Actor.empty());

        TestKitSettings testKitSettings = TestKitSettings.apply(actorSystem);

        TestProbe<AssemblyComponentLifecycleMessage> supervisorProbe = new TestProbe<AssemblyComponentLifecycleMessage>(
                "supervisor-probe",
                actorSystem,
                testKitSettings);

        Set<ConnectionType> componentType = Collections.singleton(JConnectionType.AkkaType);
        Set<Connection> connections = Collections.EMPTY_SET;
        AssemblyInfo assemblyInfo = JAssemblyInfoFactory.make("trombone-assembly", "wfos", "csw.common.components.assembly.JSampleAssembly", JComponent.DoNotRegister, componentType, connections);

        Behavior<Nothing$> behavior = new JSampleAssemblyFactory(JAssemblyDomainMessages.class).behaviour(assemblyInfo, supervisorProbe.ref()).narrow();

        Future<ActorRef<Nothing$>> assemblyMsgActorRefF = actorSystem.systemActorOf(behavior, "assembly", Props.empty(), Timeout.apply(5, TimeUnit.SECONDS));
        ActorRef<Nothing$> assemblyMsgActorRef = Await.result(assemblyMsgActorRefF, Duration.create(5, TimeUnit.SECONDS));

        ClassTag<Initialized> initializedClassTag = JClassTag.make(Initialized.class);
        Initialized initialized = supervisorProbe.expectMsgType(FiniteDuration.apply(5, TimeUnit.SECONDS), initializedClassTag);

        Assert.assertEquals(assemblyMsgActorRef, initialized.assemblyRef());

        ActorRef<Running> replyTo = supervisorProbe.ref().narrow();
        initialized.assemblyRef().tell(InitialAssemblyMsg.Run$.MODULE$);

        ClassTag<Running> runningClassTag = JClassTag.make(Running.class);
        Running running = supervisorProbe.expectMsgType(FiniteDuration.apply(5, TimeUnit.SECONDS), runningClassTag);

        Assert.assertEquals(assemblyMsgActorRef, running.assemblyRef());
    }
}
