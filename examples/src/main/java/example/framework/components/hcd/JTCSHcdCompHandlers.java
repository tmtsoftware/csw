package example.framework.components.hcd;

import akka.actor.typed.javadsl.ActorContext;
import csw.command.client.CommandResponseManager;
import csw.command.client.messages.TopLevelActorMessage;
import csw.framework.javadsl.JDefaultComponentHandlers;
import csw.framework.models.JCswContext;
import csw.params.commands.CommandResponse;
import csw.params.commands.ControlCommand;
import csw.params.core.models.Id;
import csw.time.core.models.UTCTime;
import csw.time.scheduler.api.TimeServiceScheduler;
import scala.concurrent.duration.FiniteDuration;

import java.util.concurrent.TimeUnit;

//#jcomponent-handlers-class
public class JTCSHcdCompHandlers extends JDefaultComponentHandlers {

    private final CommandResponseManager commandResponseManager;
    private final TimeServiceScheduler timeServiceScheduler;

    public JTCSHcdCompHandlers(ActorContext<TopLevelActorMessage> ctx, JCswContext cswCtx) {
        super(ctx, cswCtx);
        this.commandResponseManager = cswCtx.commandResponseManager();
        this.timeServiceScheduler = cswCtx.timeServiceScheduler();
    }
    //#jcomponent-handlers-class

    @Override
    public CommandResponse.SubmitResponse onSubmit(Id runId, ControlCommand controlCommand) {
        if (controlCommand.commandName().toString().equals("move")) {
            this.timeServiceScheduler.scheduleOnce(UTCTime.after(FiniteDuration.apply(5, TimeUnit.SECONDS)), () -> this.commandResponseManager.updateCommand(new CommandResponse.Completed(runId)));
            return new CommandResponse.Started(runId);
        } else return new CommandResponse.Completed(runId);
    }
}
