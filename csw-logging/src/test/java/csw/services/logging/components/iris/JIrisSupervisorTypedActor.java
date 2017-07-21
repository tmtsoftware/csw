package csw.services.logging.components.iris;

import akka.typed.Behavior;
import akka.typed.javadsl.Actor;
import akka.typed.javadsl.Actor.MutableBehavior;
import akka.typed.javadsl.ActorContext;
import akka.typed.javadsl.ReceiveBuilder;
import csw.services.logging.javadsl.ILogger;
import csw.services.logging.LogCommand;

public class JIrisSupervisorTypedActor extends JIrisTypedActorLogger<LogCommand> {

    private ActorContext<LogCommand> actorContext = null;
    private ILogger log = getLogger();

    private JIrisSupervisorTypedActor(ActorContext<LogCommand> actorContext) {
        super(actorContext);
        this.actorContext = actorContext;
    }

    public static <LogCommand> Behavior<LogCommand> irisBeh() {
        return Actor.mutable(ctx -> (MutableBehavior<LogCommand>) new JIrisSupervisorTypedActor((ActorContext<csw.services.logging.LogCommand>) ctx));
    }

    @Override
    public Actor.Receive<LogCommand> createReceive() {

        ReceiveBuilder<LogCommand> builder = receiveBuilder()
                .onMessage(LogCommand.class,
                        command -> command == LogCommand.LogTrace$.MODULE$,
                        command -> {
                                log.trace(command.toString());
                                return Actor.same();
                        })
                .onMessage(LogCommand.class,
                        command -> command == LogCommand.LogDebug$.MODULE$,
                        command -> {
                            log.debug(command.toString());
                            return Actor.same();
                        })
                .onMessage(LogCommand.class,
                        command -> command == LogCommand.LogInfo$.MODULE$,
                        command -> {
                            log.info(command.toString());
                            return Actor.same();
                        })
                .onMessage(LogCommand.class,
                        command -> command == LogCommand.LogWarn$.MODULE$,
                        command -> {
                            log.warn(command.toString());
                            return Actor.same();
                        })
                .onMessage(LogCommand.class,
                        command -> command == LogCommand.LogError$.MODULE$,
                        command -> {
                            log.error(command.toString());
                            return Actor.same();
                        })
                .onMessage(LogCommand.class,
                        command -> command == LogCommand.LogFatal$.MODULE$,
                        command -> {
                            log.fatal(command.toString());
                            return Actor.same();
                        });
        return builder.build();
    }
}
