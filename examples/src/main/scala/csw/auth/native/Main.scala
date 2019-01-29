package csw.auth.native

import akka.actor.ActorSystem
import csw.aas.native.api.NativeAppAuthAdapter
import csw.auth.native.commands._
import csw.location.client.utils.LocationServerStatus
import org.backuity.clist._

// #main-app
object Main extends App {

  LocationServerStatus.requireUpLocally()

  implicit val actorSystem: ActorSystem = ActorSystem()
  val adapter: NativeAppAuthAdapter     = AdapterFactory.makeAdapter

  val command: Option[AppCommand] = Cli
    .parse(args)
    .withProgramName("demo-cli")
    .withCommands(
      new LoginCommand(adapter),
      new LogoutCommand(adapter),
      new ReadCommand,
      new WriteCommand(adapter)
    )

  try {
    command.foreach(_.run())
  } finally {
    actorSystem.terminate()
  }
}
// #main-app
