package csw.services.location.internal

import akka.stream.KillSwitch
import akka.stream.scaladsl.{Keep, Sink, Source}
import csw.services.location.models.{Connection, Location, Removed, Resolved}
import csw.services.location.scaladsl.ActorRuntime

import scala.concurrent.Future

class LocationBroadcast(broadcast: Source[Location, KillSwitch], actorRuntime: ActorRuntime) {

  import actorRuntime._

  def removed(connection: Connection): Future[Removed] = find {
    case x: Removed if x.connection == connection => x
  }

  def resolved(connection: Connection): Future[Resolved] = find {
    case x: Resolved if x.connection == connection => x
  }

  def filter(connection: Connection): Source[Location, KillSwitch] = {
    broadcast.filter(_.connection == connection)
  }

  private def find[T](pf: PartialFunction[Location, T]): Future[T] = {
    val (switch, locationF) = broadcast
      .collect(pf)
      .toMat(Sink.head)(Keep.both).run()

    locationF.onComplete(_ => switch.shutdown())
    locationF
  }

}
