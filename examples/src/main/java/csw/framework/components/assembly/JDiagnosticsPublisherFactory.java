package csw.framework.components.assembly;

import akka.typed.ActorRef;
import akka.typed.Behavior;
import akka.typed.javadsl.Actor;
import csw.services.command.javadsl.JCommandService;

public class JDiagnosticsPublisherFactory {
    public static Behavior<DiagnosticPublisherMessages> make(JCommandService componentCommandService, ActorRef<WorkerActorMsg> workerActor) {
        return Actor.mutable(ctx -> new JDiagnosticsPublisher(ctx, componentCommandService, workerActor));
    }
}
