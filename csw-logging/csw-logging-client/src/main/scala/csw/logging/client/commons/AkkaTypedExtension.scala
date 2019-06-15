package csw.logging.client.commons

import akka.actor.typed.Scheduler
import akka.actor.typed.SpawnProtocol.Spawn
import akka.actor.typed._
import akka.actor.typed.scaladsl.AskPattern._
import akka.util.Timeout

import scala.concurrent.Await
import scala.concurrent.duration.{DurationInt, FiniteDuration}

object AkkaTypedExtension {
  implicit class UserActorFactory(system: ActorSystem[SpawnProtocol]) {
    private val defaultDuration: FiniteDuration = 5.seconds
    private implicit val timeout: Timeout       = Timeout(defaultDuration)
    private implicit val scheduler: Scheduler   = system.scheduler

    def spawn[T](behavior: Behavior[T], name: String, props: Props = Props.empty): ActorRef[T] = {
      Await.result(system ? Spawn(behavior, name, props), defaultDuration)
    }
  }
}
