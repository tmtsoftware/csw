package csw.services.location.internal

import akka.cluster.Cluster
import akka.cluster.ddata.Replicator._
import akka.cluster.ddata._
import csw.services.location.commons.Constants
import csw.services.location.models.{Connection, Location}

import scala.concurrent.duration.DurationDouble

/**
  * Registry is used to create CRDTs and manage its update and get messages for replicator
  *
  * @param Key        The CRDT key
  * @param EmptyValue The default value of CRDT key
  * @tparam K The type of CRDT key
  * @tparam V The type of ReplicatedData
  */
class Registry[K <: Key[V], V <: ReplicatedData](val Key: K, val EmptyValue: V) {
  /**
    * Creates an update message for replicator with WriteConsistency as majority
    *
    * @see [[akka.cluster.ddata.Replicator.Update]]
    * @param f A callback function which is passed to Replicator.Update
    */
  def update(f: V ⇒ V): Update[V] = Update(Key, EmptyValue, WriteMajority(5.seconds))(f)

  /**
    * Creates a get message for replicator with ReadConsistency as Local
    *
    * @see [[akka.cluster.ddata.Replicator.Get]]
    */
  def get: Get[V] = Get(Key, ReadLocal)
}

object Registry {

  /**
    * AllServices manages CRDT with Constants.RegistryKey as key and a map of connection to location as value. It is used to get
    * list of all registered locations. Since there is no other way to get that data from CRDT, Constants.RegistryKey is created
    * which will hold all locations registered with CRDT.
    */
  object AllServices extends Registry[LWWMapKey[Connection, Location], LWWMap[Connection, Location]](
    Key = LWWMapKey(Constants.RegistryKey),
    EmptyValue = LWWMap.empty
  )

  /**
    * Service manages CRDT with connection as key and location as value. It is used to track a connection by subscribing events
    * on it.
    */
  class Service(connection: Connection)(implicit cluster: Cluster) extends Registry[LWWRegisterKey[Option[Location]], LWWRegister[Option[Location]]](
    Key = LWWRegisterKey(connection.name),
    EmptyValue = LWWRegister(Option.empty)
  )

}
