package csw.auth.native

import akka.actor.ActorSystem
import csw.aas.native.api.NativeAppAuthAdapter
import csw.auth.native.commands._
import csw.location.client.utils.LocationServerStatus

// #main-app
object Main extends App {

  LocationServerStatus.requireUpLocally()

  implicit val actorSystem: ActorSystem = ActorSystem()
  val adapter: NativeAppAuthAdapter     = AdapterFactory.makeAdapter

  val command = CommandFactory.make(adapter, args)

  try {
    command.run()
  } finally {
    actorSystem.terminate()
  }
}
// #main-app
