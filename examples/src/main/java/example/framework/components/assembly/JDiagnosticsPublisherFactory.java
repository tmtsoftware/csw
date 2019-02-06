package example.framework.components.assembly;

import akka.actor.typed.ActorRef;
import akka.actor.typed.Behavior;
import akka.actor.typed.javadsl.Behaviors;
import csw.command.api.javadsl.ICommandService;
import example.framework.components.assembly.DiagnosticPublisherMessages;
import example.framework.components.assembly.WorkerActorMsg;

public class JDiagnosticsPublisherFactory {
    public static Behavior<DiagnosticPublisherMessages> make(ICommandService componentCommandService, ActorRef<WorkerActorMsg> workerActor) {
        return Behaviors.setup(ctx -> new JDiagnosticsPublisher(ctx, componentCommandService, workerActor));
    }
}
