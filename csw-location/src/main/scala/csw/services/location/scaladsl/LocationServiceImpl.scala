package csw.services.location.scaladsl

import java.net.URI
import javax.jmdns.{JmDNS, ServiceInfo}

import akka.Done
import akka.actor.{ActorRef, ActorSystem}
import akka.serialization.Serialization
import akka.stream.KillSwitch
import akka.stream.scaladsl.Source
import csw.services.location.common.ActorRuntime
import csw.services.location.common.ServiceInfoExtensions.RichServiceInfo
import csw.services.location.models._

import collection.JavaConverters._
import scala.concurrent.Future

private class LocationServiceImpl(
  jmDNS: JmDNS,
  actorRuntime: ActorRuntime,
  jmDnsEventStream: JmDnsEventStream
) extends LocationService { outer =>

  private val jmDnsDispatcher = actorRuntime.actorSystem.dispatchers.lookup("jmdns.dispatcher")

  override def register(reg: TcpRegistration): Future[RegistrationResult] = Future {
    jmDNS.registerService(
      ServiceInfo.create(LocationService.DnsType, reg.connection.toString, reg.port, "")
    )
    registrationResult(reg.connection)
  }(jmDnsDispatcher)

  override def register(reg: HttpRegistration): Future[RegistrationResult] = Future {
    val values = Map(LocationService.PathKey -> reg.path).asJava
    jmDNS.registerService(
      ServiceInfo.create(LocationService.DnsType, reg.connection.toString, reg.port, 0, 0, values)
    )
    registrationResult(reg.connection)
  }(jmDnsDispatcher)

  override def register(reg: AkkaRegistration): Future[RegistrationResult] = Future {
    val uri = new URI(Serialization.serializedActorPath(reg.component))
    val values = Map(
      LocationService.PathKey -> uri.getPath,
      LocationService.SystemKey -> uri.getUserInfo,
      LocationService.PrefixKey -> reg.prefix
    ).asJava
    jmDNS.registerService(
      ServiceInfo.create(LocationService.DnsType, reg.connection.toString, uri.getPort, 0, 0, values)
    )
    registrationResult(reg.connection)
  }(jmDnsDispatcher)

  private def registrationResult(connection: Connection) = new RegistrationResult {
    override def componentId: ComponentId = connection.componentId

    override def unregister(): Future[Done] = outer.unregister(connection)
  }

  override def unregister(connection: Connection): Future[Done] = Future {
    jmDNS.unregisterService(ServiceInfo.create(LocationService.DnsType, connection.toString, 0, ""))
    Thread.sleep(2000)
    Done
  }(jmDnsDispatcher)

  override def unregisterAll(): Future[Done] = Future {
    jmDNS.unregisterAllServices()
    Thread.sleep(4000)
    Done
  }(jmDnsDispatcher)

  override def resolve(connections: Set[Connection]): Future[Set[Location]] = ???

  override def list: Future[List[Location]] = Future {
    jmDNS.list(LocationService.DnsType).toList.flatMap(_.locations)
  }(jmDnsDispatcher)

  override def list(componentType: ComponentType): Future[List[Location]] = ???

  override def list(hostname: String): Future[List[Location]] = ???

  override def list(connectionType: ConnectionType): Future[List[Location]] = ???

  override def track(connection: Connection): Source[Location, KillSwitch] = {
    jmDnsEventStream.source.filter(_.connection == connection)
  }

}
