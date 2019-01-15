package csw.command.client
import akka.actor.typed.ActorSystem
import csw.command.api.javadsl.ICommandService
import csw.command.api.scaladsl.CommandService
import csw.command.client.internal.{CommandServiceImpl, JCommandServiceImpl}
import csw.location.api.models.AkkaLocation

/**
 * The factory helps in creating CommandService api for scala and java both
 */
object CommandServiceFactory {

  /**
   * Make a CommandService instance for scala
   *
   * @param componentLocation the destination component location to which commands need to be sent
   * @return an instance of type CommandService
   */
  def make(componentLocation: AkkaLocation)(implicit actorSystem: ActorSystem[_]): CommandService =
    new CommandServiceImpl(componentLocation)

  /**
   * Make a CommandService instance for java
   *
   * @param componentLocation the destination component location to which commands need to be sent
   * @return an instance of type ICommandService
   */
  def jMake(componentLocation: AkkaLocation, actorSystem: ActorSystem[_]): ICommandService =
    new JCommandServiceImpl(make(componentLocation)(actorSystem))
}
