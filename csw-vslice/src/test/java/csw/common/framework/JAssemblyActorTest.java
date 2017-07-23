package csw.common.framework;

import akka.typed.ActorRef;
import akka.typed.ActorSystem;
import akka.typed.Behavior;
import akka.typed.javadsl.Adapter;
import akka.typed.testkit.TestKitSettings;
import akka.typed.testkit.scaladsl.TestProbe;
import csw.common.components.assembly.JSampleAssembly;
import csw.common.framework.javadsl.JAssemblyInfoFactory;
import csw.common.framework.models.AssemblyComponentLifecycleMessage;
import csw.common.framework.models.AssemblyComponentLifecycleMessage.*;
import csw.common.framework.models.AssemblyMsg;
import csw.common.framework.models.Component.AssemblyInfo;
import csw.common.framework.models.InitialAssemblyMsg;
import csw.common.framework.models.JComponent;
import csw.services.location.javadsl.JConnectionType;
import csw.services.location.models.Connection;
import csw.services.location.models.ConnectionType;
import org.junit.Assert;
import org.junit.Test;
import scala.concurrent.duration.FiniteDuration;
import scala.reflect.ClassTag;

import java.util.Collections;
import java.util.Set;
import java.util.concurrent.TimeUnit;

public class JAssemblyActorTest {
    @Test
    public void testAssemblyActorSendsInitializedAndRunningMessageToSupervisor() throws InterruptedException {
        akka.actor.ActorSystem untypedActorSystem = akka.actor.ActorSystem.create("untypedActorSystem");

        ActorSystem<Void> typedActorSystem = Adapter.toTyped(untypedActorSystem);
        TestKitSettings testKitSettings = TestKitSettings.apply(typedActorSystem);

        TestProbe<AssemblyComponentLifecycleMessage> supervisorProbe = new TestProbe<AssemblyComponentLifecycleMessage>(
                "supervisor-probe",
                typedActorSystem,
                testKitSettings);

        Set<ConnectionType> componentType = Collections.singleton(JConnectionType.AkkaType);
        Set<Connection> connections = Collections.EMPTY_SET;
        AssemblyInfo assemblyInfo = JAssemblyInfoFactory.make("trombone-assembly", "wfos", "csw.common.components.assembly.JSampleAssembly", JComponent.DoNotRegister, componentType, connections);

        Behavior<AssemblyMsg> behavior = JSampleAssembly.behavior(assemblyInfo, supervisorProbe.ref());

        ActorRef<AssemblyMsg> assemblyMsgActorRef = Adapter.spawn(untypedActorSystem, behavior, "assembly");

        ClassTag<Initialized> initializedClassTag = scala.reflect.ClassTag$.MODULE$.apply(Initialized.class);
        Initialized initialized = supervisorProbe.expectMsgType(FiniteDuration.apply(5, TimeUnit.SECONDS), initializedClassTag);

        Assert.assertEquals(assemblyMsgActorRef, initialized.assemblyRef());

        ActorRef<Running> replyTo = supervisorProbe.ref().narrow();
        initialized.assemblyRef().tell(new InitialAssemblyMsg.Run(replyTo));

        ClassTag<Running> runningClassTag = scala.reflect.ClassTag$.MODULE$.apply(Running.class);
        Running running = supervisorProbe.expectMsgType(FiniteDuration.apply(5, TimeUnit.SECONDS), runningClassTag);

        Assert.assertEquals(assemblyMsgActorRef, running.assemblyRef());
    }
}
