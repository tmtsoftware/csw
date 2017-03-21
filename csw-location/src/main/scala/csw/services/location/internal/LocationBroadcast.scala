package csw.services.location.internal

import akka.stream.KillSwitch
import akka.stream.scaladsl.{Keep, Sink, Source}
import csw.services.location.models.{Connection, Location, Removed, Resolved}
import csw.services.location.scaladsl.ActorRuntime

import scala.concurrent.Future
import scala.concurrent.duration.DurationLong

class LocationBroadcast(broadcast: Source[Location, KillSwitch], actorRuntime: ActorRuntime) {

  import actorRuntime._

  def removed(connection: Connection, trigger: => Unit): Future[Removed] = find(trigger) {
    case x: Removed if x.connection == connection => x
  }

  def resolved(connection: Connection, trigger: => Unit): Future[Resolved] = find(trigger) {
    case x: Resolved if x.connection == connection => x
  }

  def filter(connection: Connection): Source[Location, KillSwitch] = {
    broadcast.filter(_.connection == connection)
  }

  private def find[T](trigger: => Unit)(pf: PartialFunction[Location, T]): Future[T] = {
    implicit val mat = actorRuntime.makeMat()
    val (switch, locationF) = broadcast
      .collect(pf)
      .toMat(Sink.head)(Keep.both).run()

    locationF.onComplete(_ => switch.shutdown())
    Source.single(()).delay(10.millis).runForeach(_ => trigger)
    locationF
  }
}
