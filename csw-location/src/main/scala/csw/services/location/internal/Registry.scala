package csw.services.location.internal

import akka.cluster.Cluster
import akka.cluster.ddata.Replicator.{Get, ReadMajority, Update, WriteMajority}
import akka.cluster.ddata._
import csw.services.location.models.{Connection, Location}

import scala.concurrent.duration.DurationDouble

class Registry[K <: Key[V], V <: ReplicatedData](val Key: K, val EmptyValue: V) {
  def update(f: V ⇒ V): Update[V] = Update(Key, EmptyValue, WriteMajority(5.seconds))(f)
  def get: Get[V] = Get(Key, ReadMajority(5.seconds))
}

object Registry {
  object AllServices extends Registry[LWWMapKey[Connection, Location], LWWMap[Connection, Location]](
    Key = LWWMapKey(Constants.RegistryKey),
    EmptyValue = LWWMap.empty
  )

  class Service(connection: Connection)(implicit cluster: Cluster) extends Registry[LWWRegisterKey[Option[Location]], LWWRegister[Option[Location]]](
    Key = LWWRegisterKey(connection.name),
    EmptyValue = LWWRegister(Option.empty)
  )
}
