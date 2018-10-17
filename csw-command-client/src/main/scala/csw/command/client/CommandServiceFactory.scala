package csw.command.client
import akka.actor.typed.ActorSystem
import csw.command.api.javadsl.ICommandService
import csw.command.api.scaladsl.CommandService
import csw.command.client.internal.{CommandServiceImpl, JCommandServiceImpl}
import csw.location.api.models.AkkaLocation

object CommandServiceFactory {

  def make(componentLocation: AkkaLocation)(implicit actorSystem: ActorSystem[_]): CommandService =
    new CommandServiceImpl(componentLocation)

  def jMake(componentLocation: AkkaLocation, actorSystem: ActorSystem[_]): ICommandService =
    new JCommandServiceImpl(make(componentLocation)(actorSystem))
}
