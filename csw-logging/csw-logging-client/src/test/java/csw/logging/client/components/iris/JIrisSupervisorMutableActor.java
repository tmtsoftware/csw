package csw.logging.client.components.iris;

import akka.actor.typed.Behavior;
import akka.actor.typed.javadsl.BehaviorBuilder;
import akka.actor.typed.javadsl.Behaviors;
import csw.logging.api.javadsl.ILogger;
import csw.logging.client.LogCommand;
import csw.logging.client.javadsl.JLoggerFactory;

public class JIrisSupervisorMutableActor {

    public static Behavior<LogCommand> irisBeh(String componentName) {
        JLoggerFactory loggerFactory = new JLoggerFactory(componentName);
        return new JIrisSupervisorMutableActor().behavior(loggerFactory);
    }

    public Behavior<LogCommand> behavior(JLoggerFactory loggerFactory){

        return Behaviors.setup(actorContext -> {
            ILogger log = loggerFactory.getLogger(actorContext, getClass());

            return BehaviorBuilder.<LogCommand>create()
                    .onMessage(LogCommand.class,
                            command -> command == LogCommand.LogTrace$.MODULE$,
                            (ctx,command) -> {
                                log.trace(command.toString());
                                return Behaviors.same();
                            })
                    .onMessage(LogCommand.class,
                            command -> command == LogCommand.LogDebug$.MODULE$,
                            (ctx,command) -> {
                                log.debug(command.toString());
                                return Behavior.same();
                            })
                    .onMessage(LogCommand.class,
                            command -> command == LogCommand.LogInfo$.MODULE$,
                            (ctx,command) -> {
                                log.info(command.toString());
                                return Behavior.same();
                            })
                    .onMessage(LogCommand.class,
                            command -> command == LogCommand.LogWarn$.MODULE$,
                            (ctx,command) -> {
                                log.warn(command.toString());
                                return Behavior.same();
                            })
                    .onMessage(LogCommand.class,
                            command -> command == LogCommand.LogError$.MODULE$,
                            (ctx,command) -> {
                                log.error(command.toString());
                                return Behavior.same();
                            })
                    .onMessage(LogCommand.class,
                            command -> command == LogCommand.LogFatal$.MODULE$,
                            (ctx,command) -> {
                                log.fatal(command.toString());
                                return Behavior.same();
                            })
                    .build();
        });
    }
}
