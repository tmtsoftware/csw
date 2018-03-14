package csw.framework.components.assembly;

import akka.typed.ActorRef;
import akka.typed.javadsl.Actor;
import akka.typed.javadsl.ActorContext;
import csw.services.command.javadsl.JCommandService;

public class JDiagnosticsPublisher extends Actor.MutableBehavior<DiagnosticPublisherMessages> {

    private ActorContext<DiagnosticPublisherMessages> ctx;
    JCommandService runningComponent;
    ActorRef<WorkerActorMsg> worker;

    public JDiagnosticsPublisher(ActorContext<DiagnosticPublisherMessages> context, JCommandService componentCommandService, ActorRef<WorkerActorMsg> workerActor) {
        ctx = context;
        runningComponent = componentCommandService;
        worker = workerActor;
    }

    @Override
    public Actor.Receive<DiagnosticPublisherMessages> createReceive() {
        return null;
    }
}
