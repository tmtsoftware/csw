package example.auth.installed

import akka.actor.ActorSystem
import csw.aas.installed.api.InstalledAppAuthAdapter

import csw.location.client.utils.LocationServerStatus
import example.auth.installed.commands.{AppCommand, CommandFactory}

// #main-app
object Main extends App {

  LocationServerStatus.requireUpLocally()

  implicit val actorSystem: ActorSystem = ActorSystem()

  val adapter: InstalledAppAuthAdapter = AdapterFactory.makeAdapter

  val command: Option[AppCommand] = CommandFactory.makeCommand(adapter, args)

  try {
    command.foreach(_.run())
  } finally {
    actorSystem.terminate()
  }
}
// #main-app
