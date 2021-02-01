package example.tutorial.basic.samplehcd;

import akka.actor.Cancellable;
import akka.actor.typed.ActorRef;
import akka.actor.typed.internal.adapter.ActorRefAdapter;
import akka.actor.typed.javadsl.ActorContext;
import akka.actor.typed.javadsl.Behaviors;
import csw.command.client.messages.TopLevelActorMessage;
import csw.framework.javadsl.JComponentHandlers;
import csw.framework.models.JCswContext;
import csw.location.api.models.TrackingEvent;
import csw.logging.api.javadsl.ILogger;
import csw.params.commands.ControlCommand;
import csw.params.commands.Result;
import csw.params.commands.Setup;
import csw.params.core.generics.Parameter;
import csw.params.core.models.Id;
import csw.params.events.Event;
import csw.params.events.EventName;
import csw.params.events.SystemEvent;
import csw.params.javadsl.JKeyType;
import csw.prefix.models.Prefix;
import csw.time.core.models.UTCTime;
import scala.concurrent.duration.FiniteDuration;

import java.util.Optional;

import static csw.params.commands.CommandIssue.UnsupportedCommandIssue;
import static csw.params.commands.CommandResponse.*;
import static example.tutorial.basic.shared.JSampleInfo.*;
import static java.util.concurrent.TimeUnit.MILLISECONDS;

/**
 * Domain specific logic should be written in below handlers.
 * These handlers get invoked when component receives messages/commands from other component/entity.
 * For example, if one component sends Submit(Setup(args)) command to SampleHcd, these will be first validated in the
 * supervisor and then forwarded to Component TLA which first invokes validateCommand hook
 * and if validation is successful, then onSubmit hook gets invoked.
 * You can find more information on this here : https://tmtsoftware.github.io/csw/framework.html
 */
@SuppressWarnings({"OptionalUsedAsFieldOrParameterType", "unused"})
public class JSampleHcdHandlers extends JComponentHandlers {

    private final JCswContext cswCtx;
    private final ILogger log;
    private final Prefix prefix;
    private final ActorContext<TopLevelActorMessage> ctx;

    JSampleHcdHandlers(ActorContext<TopLevelActorMessage> ctx, JCswContext cswCtx) {
        super(ctx, cswCtx);
        this.ctx = ctx;
        this.cswCtx = cswCtx;
        this.log = cswCtx.loggerFactory().getLogger(getClass());
        prefix = cswCtx.componentInfo().prefix();
    }

    //#initialize
    private Optional<Cancellable> maybePublishingGenerator = Optional.empty();

    @Override
    public void initialize() {
        log.info("In HCD initialize");
        maybePublishingGenerator = Optional.of(publishCounter());
    }

    @Override
    public void onLocationTrackingEvent(TrackingEvent trackingEvent) {
        log.debug(() -> "TrackingEvent received: " + trackingEvent.connection().name());
    }

    @Override
    public void onShutdown() {
        log.info("HCD is shutting down");
    }
    //#initialize

    //#publish
    private int counter = 0;

    private Optional<Event> incrementCounterEvent() {
        counter += 1;
        Parameter<Integer> param = JKeyType.IntKey().make("counter").set(counter);
        return Optional.of(new SystemEvent(cswCtx.componentInfo().prefix(), new EventName("HcdCounter")).add(param));
    }

    private Cancellable publishCounter() {
        log.info("HCD: " + prefix + " started publishing stream.");
        return cswCtx.eventService().defaultPublisher().publish(this::incrementCounterEvent, java.time.Duration.ofSeconds(2));
    }

    private void stopPublishingGenerator() {
        log.info("HCD: " + prefix + " stops publishing stream");
        maybePublishingGenerator.ifPresent(Cancellable::cancel);
    }
    //#publish

    //#validate
    @Override
    public ValidateCommandResponse validateCommand(Id runId, ControlCommand command) {
        if (command.commandName().equals(hcdSleep) || command.commandName().equals(hcdImmediate)) {
            return new Accepted(runId);
        }
        log.error("HCD: " + prefix + " received an unsupported command: " + command.commandName().name());
        return new Invalid(runId, new UnsupportedCommandIssue("Command " + command.commandName().name() + " is not supported fpr HCD: " + prefix + "."));
    }
    //#validate

    //#onSetup
    @Override
    public SubmitResponse onSubmit(Id runId, ControlCommand command) {
        log.info(() -> "HCD: " + prefix + " handling command: " + command.commandName());

        if (command instanceof Setup)
            return onSetup(runId, (Setup) command);
        // implement (or not)
        return new Invalid(runId, new UnsupportedCommandIssue("HCD: " + prefix + " only supports Setup commands"));
    }

    private SubmitResponse onSetup(Id runId, Setup setup) {
        log.info("HCD: " + prefix + " onSubmit received command: " + setup);
        if (setup.commandName().equals(hcdSleep)) {
            Long sleepTime = setup.apply(sleepTimeKey).head();
            ActorRef<WorkerCommand> worker = createWorkerActor();
            worker.tell(new Sleep(runId, sleepTime));
            return new Started(runId);
        } else if (setup.commandName().equals(hcdImmediate)) {
            return new Completed(runId);
        } else
            return new Invalid(runId, new UnsupportedCommandIssue("HCD: " + prefix + " does not implement command: " + setup.commandName()));
    }
    //#onSetup


    @Override
    public void onOneway(Id runId, ControlCommand controlCommand) {
    }

    @Override
    public void onGoOffline() {
    }

    @Override
    public void onGoOnline() {
    }

    @Override
    public void onDiagnosticMode(UTCTime startTime, String hint) {
    }

    @Override
    public void onOperationsMode() {
    }

    //#worker-actor
    private interface WorkerCommand {
    }

    private static final class Sleep implements WorkerCommand {
        private final Id runId;
        private final long sleepTime;

        private Sleep(Id runId, long sleepTime) {
            this.runId = runId;
            this.sleepTime = sleepTime;
        }
    }

    private static final class Finished implements WorkerCommand {
        private final Id runId;
        private final long sleepTime;

        private Finished(Id runId, long sleepTime) {
            this.runId = runId;
            this.sleepTime = sleepTime;
        }
    }

    private ActorRef<WorkerCommand> createWorkerActor() {
        return ctx.spawnAnonymous(
                Behaviors.receive((workerCtx, msg) -> {
                    if (msg instanceof Sleep) {
                        Sleep sleep = (Sleep) msg;
                        UTCTime when = UTCTime.after(new FiniteDuration(sleep.sleepTime, MILLISECONDS));
                        cswCtx.timeServiceScheduler().scheduleOnce(when, ActorRefAdapter.toClassic(workerCtx.getSelf()), new Finished(sleep.runId, sleep.sleepTime));
                        return Behaviors.same();
                    } else if (msg instanceof Finished) {
                        Finished finished = (Finished) msg;
                        cswCtx.commandResponseManager().updateCommand(new Completed(finished.runId, new Result().madd(resultKey.set(finished.sleepTime))));
                        return Behaviors.stopped();
                    } else {
                        return Behaviors.stopped();
                    }
                })
        );
    }
    //#worker-actor
}
