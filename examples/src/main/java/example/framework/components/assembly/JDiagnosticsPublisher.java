/*
 * Copyright (c) 2022 Thirty Meter Telescope International Observatory
 * SPDX-License-Identifier: Apache-2.0
 */

package example.framework.components.assembly;

import org.apache.pekko.actor.typed.ActorRef;
import org.apache.pekko.actor.typed.Behavior;
import org.apache.pekko.actor.typed.javadsl.Behaviors;
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
