package csw.framework.components.assembly;

import akka.actor.typed.ActorRef;
import akka.actor.typed.Behavior;
import akka.actor.typed.javadsl.Behaviors;
import csw.command.javadsl.JCommandService;

public class JDiagnosticsPublisherFactory {
    public static Behavior<DiagnosticPublisherMessages> make(JCommandService componentCommandService, ActorRef<WorkerActorMsg> workerActor) {
        return Behaviors.setup(ctx -> new JDiagnosticsPublisher(ctx, componentCommandService, workerActor));
    }
}
