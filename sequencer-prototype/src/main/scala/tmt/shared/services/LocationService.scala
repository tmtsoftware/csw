package tmt.shared.services

import akka.typed.ActorSystem

import scala.concurrent.{ExecutionContext, Future}

class LocationService(actorSystem: ActorSystem[_]) {
  def resolve(name: String): ComponentRef = ComponentRef(name)
}

case class ComponentRef(name: String) {
  def setup(command: Command)(implicit ec: ExecutionContext): Future[CommandResponse] = Future {
    println(s"received $command by component=$name")
    CommandResponse(s"result of $command from component=$name")
  }
}

case class Command(name: String, params: List[Int])
case class CommandResponse(value: String)
