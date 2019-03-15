package example.framework.components.assembly;

import akka.actor.typed.ActorRef;
import akka.actor.typed.Behavior;
import akka.actor.typed.javadsl.Behaviors;
import csw.command.api.javadsl.ICommandService;

class JDiagnosticsPublisher {

    public static Behavior<DiagnosticPublisherMessages> behavior(ICommandService componentCommandService, ActorRef<WorkerActorMsg> workerActor){
        return Behaviors.setup(
                ctx -> {
                    // Setup of actor
            return Behaviors.receiveMessage( msg -> {
                // handle messages and return new behavior
                return Behaviors.same();
            });
        });
    }
}
