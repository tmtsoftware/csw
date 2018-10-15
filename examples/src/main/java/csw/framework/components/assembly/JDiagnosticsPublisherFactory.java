package csw.framework.components.assembly;

import akka.actor.typed.ActorRef;
import akka.actor.typed.Behavior;
import akka.actor.typed.javadsl.Behaviors;
import csw.command.api.javadsl.ICommandService;

public class JDiagnosticsPublisherFactory {
    public static Behavior<DiagnosticPublisherMessages> make(ICommandService componentCommandService, ActorRef<WorkerActorMsg> workerActor) {
        return Behaviors.setup(ctx -> new JDiagnosticsPublisher(ctx, componentCommandService, workerActor));
    }
}
