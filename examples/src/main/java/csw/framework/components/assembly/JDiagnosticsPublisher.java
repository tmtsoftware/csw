package csw.framework.components.assembly;

import akka.actor.typed.ActorRef;
import akka.actor.typed.javadsl.ActorContext;
import akka.actor.typed.javadsl.Behaviors;
import akka.actor.typed.javadsl.MutableBehavior;
import csw.command.javadsl.JCommandService;

public class JDiagnosticsPublisher extends MutableBehavior<DiagnosticPublisherMessages> {

    private ActorContext<DiagnosticPublisherMessages> ctx;
    JCommandService runningComponent;
    ActorRef<WorkerActorMsg> worker;

    public JDiagnosticsPublisher(ActorContext<DiagnosticPublisherMessages> context, JCommandService componentCommandService, ActorRef<WorkerActorMsg> workerActor) {
        ctx = context;
        runningComponent = componentCommandService;
        worker = workerActor;
    }

    @Override
    public Behaviors.Receive<DiagnosticPublisherMessages> createReceive() {
        return null;
    }
}
