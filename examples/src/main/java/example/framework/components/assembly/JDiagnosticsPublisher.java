package example.framework.components.assembly;

import akka.actor.typed.ActorRef;
import akka.actor.typed.javadsl.AbstractBehavior;
import akka.actor.typed.javadsl.ActorContext;
import akka.actor.typed.javadsl.Behaviors;
import akka.actor.typed.javadsl.Receive;
import csw.command.api.javadsl.ICommandService;
import example.framework.components.assembly.DiagnosticPublisherMessages;
import example.framework.components.assembly.WorkerActorMsg;

public class JDiagnosticsPublisher extends AbstractBehavior<DiagnosticPublisherMessages> {

    private ActorContext<DiagnosticPublisherMessages> ctx;
    ICommandService runningComponent;
    ActorRef<WorkerActorMsg> worker;

    public JDiagnosticsPublisher(ActorContext<DiagnosticPublisherMessages> context, ICommandService componentCommandService, ActorRef<WorkerActorMsg> workerActor) {
        ctx = context;
        runningComponent = componentCommandService;
        worker = workerActor;
    }

    @Override
    public Receive<DiagnosticPublisherMessages> createReceive() {
        return null;
    }
}
