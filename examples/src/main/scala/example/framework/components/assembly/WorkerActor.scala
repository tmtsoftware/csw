package example.framework.components.assembly

import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, Behavior}
import csw.config.api.models.ConfigData
import example.framework.components.assembly.WorkerActorMsgs.{GetStatistics, InitialState}

trait WorkerActorMsg
object WorkerActorMsgs {
  case class InitialState(replyTo: ActorRef[Int])      extends WorkerActorMsg
  case class JInitialState(replyTo: ActorRef[Integer]) extends WorkerActorMsg
  case class GetStatistics(replyTo: ActorRef[Int])     extends WorkerActorMsg
}

object WorkerActor {

  def behavior(configData: ConfigData): Behavior[WorkerActorMsg] = Behaviors.setup { ctx =>
    // some setup required for the actor could be done here

    def receive(state: Int): Behavior[WorkerActorMsg] = Behaviors.receiveMessage {
      case _: InitialState  => receive(0) // do something and return new behavior with the changed state
      case _: GetStatistics => receive(1) // do something else
    }

    receive(-1)

  }
}
