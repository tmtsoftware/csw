package csw.framework.components.assembly;

import akka.typed.ActorRef;
import akka.typed.Behavior;
import akka.typed.javadsl.Actor;
import csw.services.command.javadsl.JCommandService;

import java.util.Optional;

public class JDiagnosticsPublisherFactory {
    public static Behavior<DiagnosticPublisherMessages> make(Optional<JCommandService> componentCommandService, Optional<ActorRef<WorkerActorMsg>> workerActor) {
        return Actor.mutable(ctx -> new JDiagnosticsPublisher(ctx, componentCommandService, workerActor));
    }
}
