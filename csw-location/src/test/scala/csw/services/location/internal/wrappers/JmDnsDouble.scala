package csw.services.location.internal.wrappers

import java.net.URI

import akka.actor.ActorPath
import akka.serialization.Serialization
import akka.stream.scaladsl.SourceQueueWithComplete
import csw.services.location.models._
import csw.services.location.scaladsl.ActorRuntime

class JmDnsDouble(actorRuntime: ActorRuntime, queue: SourceQueueWithComplete[Location]) extends JmDnsApi {

  var state: Map[Connection, Location] = Map.empty

  override def registerService(reg: Registration): Unit = {
    val location = reg match {
      case TcpRegistration(connection, port)              =>
        val uri = new URI(s"tcp://${actorRuntime.ipaddr.getHostAddress}:$port")
        ResolvedTcpLocation(connection, uri)
      case HttpRegistration(connection, port, path)       =>
        val uri = new URI(s"http://${actorRuntime.ipaddr.getHostAddress}:$port/$path")
        ResolvedHttpLocation(connection, uri, path)
      case AkkaRegistration(connection, actorRef, prefix) =>
        val actorPath = ActorPath.fromString(Serialization.serializedActorPath(actorRef))
        val uri = new URI(actorPath.toString)
        ResolvedAkkaLocation(connection, uri, prefix, Some(actorRef))
    }

    queue.offer(Unresolved(reg.connection))
    queue.offer(location)
    state += (reg.connection -> location)
  }

  override def unregisterService(connection: Connection): Unit = {
    queue.offer(Removed(connection))
    state -= connection
  }

  override def unregisterAllServices(): Unit = {
    state.mapValues(location => queue.offer(Removed(location.connection)))
    state = Map.empty
  }

  override def requestServiceInfo(connection: Connection): Unit = ()

  override def list(typeName: String): List[Location] = state.values.toList

  override def close(): Unit = ()
}

object JmDnsDouble extends JmDnsApiFactory {
  def make(actorRuntime: ActorRuntime, queue: SourceQueueWithComplete[Location]): JmDnsApi = {
    new JmDnsDouble(actorRuntime, queue)
  }
}
