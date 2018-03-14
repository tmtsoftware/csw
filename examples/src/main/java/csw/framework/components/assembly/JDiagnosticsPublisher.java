package csw.framework.components.assembly;

import akka.typed.javadsl.ActorContext;
import akka.typed.ActorRef;
import akka.typed.javadsl.Actor;
import csw.services.command.javadsl.JCommandService;

import java.util.Optional;

public class JDiagnosticsPublisher extends Actor.MutableBehavior<DiagnosticPublisherMessages> {

    private ActorContext<DiagnosticPublisherMessages> ctx;
    Optional<JCommandService> runningComponent;
    Optional<ActorRef<WorkerActorMsg>> worker;

    public JDiagnosticsPublisher(ActorContext<DiagnosticPublisherMessages> context, Optional<JCommandService> componentCommandService, Optional<ActorRef<WorkerActorMsg>> workerActor) {
        ctx = context;
        runningComponent = componentCommandService;
        worker = workerActor;
    }

    @Override
    public Actor.Receive<DiagnosticPublisherMessages> createReceive() {
        return null;
    }
}
