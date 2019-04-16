package csw.logging.client.commons

import akka.actor.typed._
import akka.util.Timeout

import scala.concurrent.Await
import scala.concurrent.duration.{DurationInt, FiniteDuration}

object AkkaTypedExtension {
  implicit class UserActorFactory(system: ActorSystem[_]) {
    private val defaultDuration: FiniteDuration = 5.seconds
    private implicit val timeout: Timeout       = Timeout(defaultDuration)

    def spawn[T](behavior: Behavior[T], name: String, props: Props = Props.empty): ActorRef[T] = {
      Await.result(system.systemActorOf(behavior, name, props), defaultDuration)
    }
  }
}
