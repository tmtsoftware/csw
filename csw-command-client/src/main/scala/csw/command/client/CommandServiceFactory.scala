package csw.command.client
import akka.actor.typed.ActorSystem
import csw.command.api.javadsl.ICommandService
import csw.command.api.scaladsl.CommandService
import csw.location.api.models.AkkaLocation

object CommandServiceFactory {

  def make(componentLocation: AkkaLocation)(implicit actorSystem: ActorSystem[_]): CommandService =
    CommandServiceFactory.make(componentLocation)

  def jMake(componentLocation: AkkaLocation, actorSystem: ActorSystem[_]): ICommandService =
    CommandServiceFactory.jMake(componentLocation, actorSystem)
}
