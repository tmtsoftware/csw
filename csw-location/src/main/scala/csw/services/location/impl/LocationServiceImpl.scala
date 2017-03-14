package csw.services.location.impl

import javax.jmdns.{JmDNS, ServiceInfo}

import akka.Done
import akka.stream.KillSwitch
import akka.stream.scaladsl.Source
import csw.services.location.scaladsl.{ActorRuntime, LocationService}
import csw.services.location.common.Constants
import csw.services.location.scaladsl.models.ServiceInfoExtensions.RichServiceInfo
import csw.services.location.scaladsl.models._

import scala.async.Async._
import scala.concurrent.Future

private[location] class LocationServiceImpl(
  jmDNS: JmDNS,
  actorRuntime: ActorRuntime,
  jmDnsEventStream: JmDnsEventStream
) extends LocationService { outer =>

  import actorRuntime._

  private val jmDnsDispatcher = actorRuntime.actorSystem.dispatchers.lookup("jmdns.dispatcher")

  override def register(reg: Registration): Future[RegistrationResult] = async {
    await(list).find(_.connection == reg.connection) match {
      case Some(_) => throw new IllegalStateException(s"A service with name ${reg.connection.name} is already registered")
      case None => await(registerUniqueService(reg))
    }
  }

  override def unregister(connection: Connection): Future[Done] = {
    val locationF = jmDnsEventStream.broadcast.removed(connection)
    jmDNS.unregisterService(ServiceInfo.create(Constants.DnsType, connection.name, 0, ""))
    locationF.map(_ => Done)
  }

  override def unregisterAll(): Future[Done] = Future {
    jmDNS.unregisterAllServices()
    Thread.sleep(4000)
    Done
  }(jmDnsDispatcher)

  override def resolve(connection: Connection): Future[Resolved] = async {
    await(list).find(_.connection == connection) match {
      case Some(location : Resolved) => location
      case _ => await(resolveService(connection))
    }
  }

  override def resolve(connections: Set[Connection]): Future[Set[Resolved]] = {
    Future.traverse(connections)(resolve)
  }

  override def list: Future[List[Location]] = Future {
    jmDNS.list(Constants.DnsType).toList.flatMap(_.locations)
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
    jmDNS.requestServiceInfo(Constants.DnsType, connection.name, true)
    jmDnsEventStream.broadcast.filter(connection)
  }

  private def registrationResult(connection: Connection) = new RegistrationResult {
    override def componentId: ComponentId = connection.componentId

    override def unregister(): Future[Done] = outer.unregister(connection)
  }

  private def registerUniqueService(reg: Registration) = Future{
    jmDNS.registerService(reg.serviceInfo)
    registrationResult(reg.connection)
  }(jmDnsDispatcher)

  private def resolveService(connection: Connection) = {
    val locationF = jmDnsEventStream.broadcast.resolved(connection)
    jmDNS.requestServiceInfo(Constants.DnsType, connection.name)
    locationF
  }
}
