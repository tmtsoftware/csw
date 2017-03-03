package csw.services.location.scaladsl

import java.net.URI
import javax.jmdns.{JmDNS, ServiceInfo}

import akka.Done
import akka.serialization.Serialization
import akka.stream.KillSwitch
import akka.stream.scaladsl.{Keep, Sink, Source}
import csw.services.location.common.ActorRuntime
import csw.services.location.common.ServiceInfoExtensions.RichServiceInfo
import csw.services.location.models._

import collection.JavaConverters._
import scala.concurrent.Future
import async.Async._

private class LocationServiceImpl(
  jmDNS: JmDNS,
  actorRuntime: ActorRuntime,
  jmDnsEventStream: JmDnsEventStream
) extends LocationService { outer =>

  import actorRuntime._

  private val jmDnsDispatcher = actorRuntime.actorSystem.dispatchers.lookup("jmdns.dispatcher")

  override def register(reg: Registration): Future[RegistrationResult] = Future {
    jmDNS.registerService(reg.serviceInfo)
    registrationResult(reg.connection)
  }(jmDnsDispatcher)

  private def registrationResult(connection: Connection) = new RegistrationResult {
    override def componentId: ComponentId = connection.componentId

    override def unregister(): Future[Done] = outer.unregister(connection)
  }

  override def unregister(connection: Connection): Future[Done] = {
    val locationF = jmDnsEventStream.broadcast.removed(connection)
    jmDNS.unregisterService(ServiceInfo.create(LocationService.DnsType, connection.toString, 0, ""))
    locationF.map(_ => Done)
  }

  override def unregisterAll(): Future[Done] = Future {
    jmDNS.unregisterAllServices()
    Thread.sleep(4000)
    Done
  }(jmDnsDispatcher)

  override def resolve(connections: Set[Connection]): Future[Set[Resolved]] = {
    Future.traverse(connections)(resolve)
  }

  override def resolve(connection: Connection): Future[Resolved] = {
    val locationF = jmDnsEventStream.broadcast.resolved(connection)
    jmDNS.requestServiceInfo(LocationService.DnsType, connection.toString)
    locationF
  }

  override def list: Future[List[Location]] = Future {
    jmDNS.list(LocationService.DnsType).toList.flatMap(_.locations)
  }(jmDnsDispatcher)

  override def list(componentType: ComponentType): Future[List[Location]] = async {
    await(list).filter(_.connection.componentId.componentType == componentType)
  }

  override def list(hostname: String): Future[List[Resolved]] = async {
    await(list).collect {
      case x: Resolved if x.uri.getHost == hostname => x
    }
  }

  override def list(connectionType: ConnectionType): Future[List[Location]] = async {
    await(list).filter(_.connection.connectionType == connectionType)
  }

  override def track(connection: Connection): Source[Location, KillSwitch] = {
    jmDNS.requestServiceInfo(LocationService.DnsType, connection.toString, true)
    jmDnsEventStream.broadcast.filter(connection)
  }

}
