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

  override def unregister(connection: Connection): Future[Done] = {
    val (switch, locationF) = jmDnsEventStream.source
      .collect {
        case x: Removed if x.connection == connection => x
      }
      .toMat(Sink.head)(Keep.both).run()

    locationF.onComplete(_ => switch.shutdown())
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
    val (switch, locationF) = jmDnsEventStream.source
      .collect {
        case x: Resolved if x.connection == connection => x
      }
      .toMat(Sink.head)(Keep.both).run()

    locationF.onComplete(_ => switch.shutdown())
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
    jmDnsEventStream.source.filter(_.connection == connection)
  }

}
