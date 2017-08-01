package csw.common.framework.javadsl.assembly;

import akka.typed.ActorRef;
import akka.typed.ActorSystem;
import akka.typed.Behavior;
import akka.typed.Props;
import akka.typed.javadsl.ActorContext;
import akka.typed.scaladsl.Actor;
import akka.typed.testkit.TestKitSettings;
import akka.typed.testkit.scaladsl.TestProbe;
import akka.util.Timeout;
import csw.common.components.assembly.AssemblyDomainMessages;
import csw.common.framework.javadsl.commons.JClassTag;
import csw.common.framework.models.AssemblyMsg;
import csw.common.framework.models.AssemblyResponseMode;
import csw.common.framework.models.Component;
import csw.common.framework.models.InitialAssemblyMsg;
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

public class JAssemblyBehaviorTest {
    private static ActorSystem system = ActorSystem.create("Assembly", Actor.empty());
    private static TestKitSettings settings = TestKitSettings.apply(system);

    private JAssemblyHandlersFactory getSampleJAssemblyFactory(JAssemblyHandlers assemblyHandlers) {
        return new JAssemblyHandlersFactory<AssemblyDomainMessages>(AssemblyDomainMessages.class) {
            @Override
            public JAssemblyHandlers<AssemblyDomainMessages> make(ActorContext<AssemblyMsg> ctx, Component.AssemblyInfo assemblyInfo) {
                return assemblyHandlers;
            }
        };
    }

    private Component.AssemblyInfo assemblyInfo = new Component.AssemblyInfo("trombone",
            "wfos",
            "csw.common.components.assembly.SampleAssembly",
            DoNotRegister,
            null,
            null);

    @AfterClass
    public static void afterAll() throws Exception {
        system.terminate();
    }

    @Test
    public void testAssemblyBehavior() throws Exception {

        JAssemblyHandlers<AssemblyDomainMessages> sampleAssemblyHandler = Mockito.mock(JAssemblyHandlers.class);
        when(sampleAssemblyHandler.initialize()).thenCallRealMethod();
        when(sampleAssemblyHandler.jInitialize()).thenReturn(CompletableFuture.completedFuture(
                BoxedUnit.UNIT
        ));

        TestProbe<AssemblyResponseMode> supervisorProbe = TestProbe.apply(system, settings);

        Timeout seconds = Timeout.durationToTimeout(FiniteDuration.apply(5, "seconds"));
        Behavior<Nothing$> behavior = getSampleJAssemblyFactory(sampleAssemblyHandler).behavior(assemblyInfo, supervisorProbe.ref());
        Future<ActorRef> assembly = system.<Nothing$>systemActorOf(behavior, "assembly", Props.empty(), seconds);
        FiniteDuration seconds1 = Duration.create(5, "seconds");
        ActorRef assemblyRef = Await.result(assembly, seconds1);

        AssemblyResponseMode.Initialized initialized = supervisorProbe.expectMsgType(JClassTag.make(AssemblyResponseMode.Initialized.class));
        Assert.assertEquals(assemblyRef, initialized.assemblyRef());

        initialized.assemblyRef().tell(InitialAssemblyMsg.Run$.MODULE$);

        AssemblyResponseMode.Running running = supervisorProbe.expectMsgType(JClassTag.make(AssemblyResponseMode.Running.class));
        verify(sampleAssemblyHandler).onRun();
        verify(sampleAssemblyHandler).isOnline_$eq(true);

        Assert.assertEquals(assemblyRef, running.assemblyRef());
    }
}
