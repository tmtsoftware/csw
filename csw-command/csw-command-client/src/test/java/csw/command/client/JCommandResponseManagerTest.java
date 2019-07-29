package csw.command.client;

import akka.actor.testkit.typed.javadsl.TestProbe;
import akka.actor.typed.ActorSystem;
import akka.actor.typed.SpawnProtocol;
import akka.util.Timeout;
import csw.command.client.messages.CommandResponseManagerMessage;
import csw.params.core.models.Id;
import org.junit.Test;
import org.scalatestplus.junit.JUnitSuite;

import java.util.concurrent.TimeUnit;

public class JCommandResponseManagerTest extends JUnitSuite {
    private ActorSystem<SpawnProtocol.Command> actorSystem = ActorSystem.apply(SpawnProtocol.create(), "java-test-command-response-manager");
    private Timeout timeOut = Timeout.apply(10, TimeUnit.SECONDS);

    @Test
    public void shouldDelegateToJQuery() {
        TestProbe<CommandResponseManagerMessage> commandResponseManagerProbe = TestProbe.create(actorSystem);
        CommandResponseManager commandResponseManager = new CommandResponseManager(commandResponseManagerProbe.getRef(), actorSystem);
        Id runId = Id.apply("1111");

        commandResponseManager.jQuery(runId, timeOut);

        commandResponseManagerProbe.expectMessageClass(CommandResponseManagerMessage.Query.class);
    }


    @Test
    public void shouldDelegateToJQueryFinal() {
        TestProbe<CommandResponseManagerMessage> commandResponseManagerProbe = TestProbe.create(actorSystem);
        CommandResponseManager commandResponseManager = new CommandResponseManager(commandResponseManagerProbe.getRef(), actorSystem);
        Id runId = Id.apply("1111");

        commandResponseManager.jQueryFinal(runId, timeOut);

        commandResponseManagerProbe.expectMessageClass(CommandResponseManagerMessage.Subscribe.class);
    }
}
