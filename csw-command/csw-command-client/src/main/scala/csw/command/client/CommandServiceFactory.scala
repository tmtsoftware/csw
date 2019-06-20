package csw.command.client
import akka.actor.typed.ActorSystem
import csw.command.api.javadsl.ICommandService
import csw.command.api.scaladsl.CommandService
import csw.command.client.internal.{CommandServiceImpl, JCommandServiceImpl}
import csw.location.api.models.AkkaLocation

/**
 * The factory helps in creating CommandService api for scala and java both
 */
trait ICommandServiceFactory {
  def make(componentLocation: AkkaLocation)(implicit actorSystem: ActorSystem[_]): CommandService
  def jMake(componentLocation: AkkaLocation, actorSystem: ActorSystem[_]): ICommandService
}

object CommandServiceFactory extends ICommandServiceFactory {

  /**
   * Make a CommandService instance for scala
   *
   * @param componentLocation the destination component location to which commands need to be sent
   * @param actorSystem of the component used for executing commands to other components and wait for the responses
   * @return an instance of type CommandService
   */
  def make(componentLocation: AkkaLocation)(implicit actorSystem: ActorSystem[_]): CommandService =
    new CommandServiceImpl(componentLocation)

  /**
   * Make a CommandService instance for java
   *
   * @param componentLocation the destination component location to which commands need to be sent
   * @param actorSystem of the component used for executing commands to other components and wait for the responses
   * @return an instance of type ICommandService
   */
  def jMake(componentLocation: AkkaLocation, actorSystem: ActorSystem[_]): ICommandService =
    new JCommandServiceImpl(make(componentLocation)(actorSystem))
}
