package org.tmt.esw.basic.sampleassembly;

import akka.actor.typed.ActorSystem;
import akka.actor.typed.javadsl.ActorContext;
import akka.util.Timeout;
import csw.command.api.javadsl.ICommandService;
import csw.command.client.CommandServiceFactory;
import csw.command.client.messages.TopLevelActorMessage;
import csw.event.api.javadsl.IEventSubscription;
import csw.framework.javadsl.JComponentHandlers;
import csw.framework.models.JCswContext;
import csw.location.api.javadsl.JComponentType;
import csw.location.models.*;
import csw.logging.api.javadsl.ILogger;
import csw.params.commands.*;
import csw.params.core.generics.Key;
import csw.params.core.models.Id;
import csw.params.events.Event;
import csw.params.events.EventKey;
import csw.params.events.EventName;
import csw.params.events.SystemEvent;
import csw.params.javadsl.JKeyType;
import csw.prefix.models.Prefix;
import csw.prefix.javadsl.JSubsystem;
import csw.time.core.models.UTCTime;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static org.tmt.esw.shared.JSampleInfo.*;
import static csw.params.commands.CommandResponse.*;
import static csw.params.commands.CommandIssue.*;
import static csw.command.client.CommandResponseManager.*;

/**
 * Domain specific logic should be written in below handlers.
 * This handlers gets invoked when component receives messages/commands from other component/entity.
 * For example, if one component sends Submit(Setup(args)) command to SampleHcd,
 * This will be first validated in the supervisor and then forwarded to Component TLA which first invokes validateCommand hook
 * and if validation is successful, then onSubmit hook gets invoked.
 * You can find more information on this here : https://tmtsoftware.github.io/csw/framework.html
 */
@SuppressWarnings({"OptionalGetWithoutIsPresent", "FieldCanBeLocal", "OptionalUsedAsFieldOrParameterType"})
public class JSampleAssemblyHandlers extends JComponentHandlers {

  private final ActorSystem<Void> system;
  private final Timeout timeout;
  private final JCswContext cswCtx;
  private final ILogger log;
  private final Prefix prefix;
  private final Connection.AkkaConnection hcdConnection;
  @SuppressWarnings("unused")
  private AkkaLocation hcdLocation = null;
  private Optional<ICommandService> hcdCS = Optional.empty();

  JSampleAssemblyHandlers(ActorContext<TopLevelActorMessage> ctx, JCswContext cswCtx) {
    super(ctx, cswCtx);
    this.cswCtx = cswCtx;
    system = ctx.getSystem();
    timeout = new Timeout(10, TimeUnit.SECONDS);
    this.log = cswCtx.loggerFactory().getLogger(getClass());
    prefix = cswCtx.componentInfo().prefix();
    hcdConnection = new Connection.AkkaConnection(new ComponentId(Prefix.apply(JSubsystem.ESW(), "samplehcd"), JComponentType.HCD()));
  }


  //#initialize
  private Optional<IEventSubscription> maybeEventSubscription = Optional.empty();

  @Override
  public CompletableFuture<Void> jInitialize() {
    log.info("Assembly: " + prefix + " initialize");
    maybeEventSubscription = Optional.of(subscribeToHcd());
    return CompletableFuture.runAsync(() -> {
    });
  }

  @Override
  public CompletableFuture<Void> jOnShutdown() {
    log.info("Assembly: " + prefix + " is shutting down.");
    return CompletableFuture.runAsync(() -> {
    });
  }
  //#initialize

  //#track-location
  //#resolve-hcd-and-create-commandservice
  @Override
  public void onLocationTrackingEvent(TrackingEvent trackingEvent) {
    log.debug(() -> "onLocationTrackingEvent called: " + trackingEvent.toString());
    if (trackingEvent instanceof LocationUpdated) {
      Location location = ((LocationUpdated) trackingEvent).location();
      hcdLocation = (AkkaLocation) location;
      hcdCS = Optional.of(CommandServiceFactory.jMake(location, system));
      onSetup(Id.apply(), new Setup(prefix, shortCommand, Optional.empty()));
    } else if (trackingEvent instanceof LocationRemoved) {
      Connection connection = trackingEvent.connection();
      if (connection == hcdConnection) {
        hcdCS = Optional.empty();
      }
    }
  }
  //#resolve-hcd-and-create-commandservice
  //#track-location

  //#subscribe
  private EventKey counterEventKey = new EventKey(Prefix.apply(JSubsystem.ESW(), "samplehcd"), new EventName("HcdCounter"));
  private Key<Integer> hcdCounterKey = JKeyType.IntKey().make("counter");

  private void processEvent(Event event) {
    log.info("Event received: " + event.eventKey());
    if (event instanceof SystemEvent) {
      SystemEvent e = (SystemEvent) event;
      if (e.eventKey().equals(counterEventKey)) {
        int counter = e.parameter(hcdCounterKey).head();
        log.info("Counter = " + counter);
      } else {
        log.warn("Unexpected event received.");
      }
    } else {
      // ObserveEvent, not expected
      log.warn("Unexpected ObserveEvent received.");
    }
  }

  private IEventSubscription subscribeToHcd() {
    log.info("Assembly " + prefix + " starting subscription.");
    return cswCtx.eventService().defaultSubscriber().subscribeCallback(Set.of(counterEventKey), this::processEvent);
  }

  @SuppressWarnings("unused")
  private void unsubscribeHcd() {
    log.info("Assembly: " + prefix + " stopping subscription.");
    maybeEventSubscription.ifPresent(IEventSubscription::unsubscribe);
  }
  //#subscribe

  //#validate
  @Override
  public ValidateCommandResponse validateCommand(Id runId, ControlCommand command) {
    CommandName cmd = command.commandName();
    if (command instanceof Setup) {
      Setup setup = (Setup) command;
      if (cmd.equals(sleep)) {
        return validateSleep(runId, setup);
      }
      if (cmd.equals(immediateCommand) || cmd.equals(shortCommand) || cmd.equals(mediumCommand) || cmd.equals(complexCommand)) {
        return new Accepted(runId);
      }
    }
    return new Invalid(runId, new UnsupportedCommandIssue("Command: " + cmd.name() + " is not supported for sample Assembly."));
  }

  private ValidateCommandResponse validateSleep(Id runId, Setup setup) {
    if (setup.exists(sleepTimeKey)) {
      Long sleepTime = setup.jGet(sleepTimeKey).get().head();
      if (sleepTime < maxSleep)
        return new Accepted(runId);
      else
        return new Invalid(runId, new ParameterValueOutOfRangeIssue("sleepTime must be < 2000"));
    } else {
      return new Invalid(runId, new MissingKeyIssue("required sleep command key: " + sleepTimeKey + " is missing."));
    }
  }
  //#validate

  @Override
  public SubmitResponse onSubmit(Id runId, ControlCommand command) {
    if (command instanceof Setup) {
      return onSetup(runId, (Setup) command);
    }
    return new Invalid(runId, new UnsupportedCommandIssue("Observe commands not supported"));
  }

  //#sending-command
  private SubmitResponse onSetup(Id runId, Setup setup) {
    CommandName cmd = setup.commandName();
    if (cmd.equals(immediateCommand)) {
      return new Completed(runId);
    }
    if (cmd.equals(shortCommand)) {
      sleepHCD(runId, setup, shortSleepPeriod);
      return new Started(runId);
    }
    if (cmd.equals(mediumCommand)) {
      sleepHCD(runId, setup, mediumSleepPeriod);
      return new Started(runId);
    }
    if (cmd.equals(longCommand)) {
      sleepHCD(runId, setup, longSleepPeriod);
      return new Started(runId);
    }
    if (cmd.equals(complexCommand)) {
      CompletableFuture<SubmitResponse> medium = simpleHCD(runId, new Setup(prefix, hcdSleep, setup.jMaybeObsId()).add(setSleepTime(mediumSleepPeriod)));
      CompletableFuture<SubmitResponse> long_ = simpleHCD(runId, new Setup(prefix, hcdSleep, setup.jMaybeObsId()).add(setSleepTime(longSleepPeriod)));
      cswCtx.commandResponseManager().queryFinalAll(Arrays.asList(medium, long_))
          .thenAccept(response -> {
            if (response instanceof OverallSuccess) {
              // Don't care about individual responses with success
              cswCtx.commandResponseManager().updateCommand(new Completed(runId));
            } else if (response instanceof OverallFailure) {
              // There must be at least one error
              List<SubmitResponse> errors = ((OverallFailure) response).getResponses().stream().filter(CommandResponse::isNegative).collect(Collectors.toList());
              cswCtx.commandResponseManager().updateCommand(errors.get(0).withRunId(runId));
            }
          }).exceptionally(ex -> {
        cswCtx.commandResponseManager().updateCommand(new CommandResponse.Error(runId, ex.toString()));
        return null;
      });
      return new Started(runId);
    }
    if (cmd.equals(sleep)) {
      sleepHCD(runId, setup, setup.apply(sleepTimeKey).head());
      return new Started(runId);
    }
    return new Invalid(runId, new CommandIssue.UnsupportedCommandIssue(setup.commandName().name()));
  }

  @SuppressWarnings("unused")
  private void sleepHCD(Id runId, Setup setup, Long sleepTime) {
    if (hcdCS.isPresent()) {
      ICommandService cs = hcdCS.get();
      Setup s = new Setup(prefix, hcdSleep, Optional.empty()).add(setSleepTime(sleepTime));
      cs.submit(s).thenAccept(submitResponse -> {
        if (submitResponse instanceof Started) {
          Started started = (Started) submitResponse;
          // Can insert extra code during execution here
          cs.queryFinal(started.runId(), timeout).thenAccept(sr -> cswCtx.commandResponseManager().updateCommand(sr.withRunId(runId)));
        } else {
          cswCtx.commandResponseManager().updateCommand(submitResponse.withRunId(runId));
        }
      });
    } else {
      cswCtx.commandResponseManager().updateCommand(
          new CommandResponse.Error(runId, "A needed HCD is not available: " + hcdConnection.componentId() + " for " + prefix)
      );
    }
  }
  //#sending-command

  private CompletableFuture<SubmitResponse> simpleHCD(Id runId, Setup setup) {
    if (hcdCS.isPresent()) {
      ICommandService cs = hcdCS.get();
      return cs.submitAndWait(setup, timeout);
    }
    return CompletableFuture.completedFuture(
        new CommandResponse.Error(runId, "A needed HCD is not available: " + hcdConnection.componentId()));
  }


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
}
