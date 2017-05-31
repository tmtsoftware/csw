package csw.services.location.internal

import akka.cluster.Cluster
import akka.cluster.ddata.Replicator._
import akka.cluster.ddata._
import csw.services.location.commons.Constants
import csw.services.location.models.{Connection, Location}

import scala.concurrent.duration.DurationDouble

/**
 * Registry is used to create distributed data and manage its update and get messages for replicator
 *
 * @param Key        The distributed data key
 * @param EmptyValue The default value of distributed data key
 * @tparam K The type of distributed data key
 * @tparam V The type of ReplicatedData
 */
class Registry[K <: Key[V], V <: ReplicatedData](val Key: K, val EmptyValue: V) {

  type Value = V

  /**
   * Creates an update message for replicator and it ensures that the response goes out only after majority nodes are written to
   *
   * @see [[akka.cluster.ddata.Replicator.Update]]
   * @param f A callback function which is passed to Replicator.Update
   */
  def update(f: V ⇒ V): Update[V] = Update(Key, EmptyValue, WriteMajority(5.seconds))(f)

  /**
   * Creates a get message for replicator. Unlike for update, it will read from the local cache of the node
   *
   * @see [[akka.cluster.ddata.Replicator.Get]]
   */
  def get: Get[V] = Get(Key, ReadLocal)
}

object Registry {

  /**
   * AllServices is a distributed map from connection to location.
   * It is used to get list of all registered locations at any point in time
   */
  object AllServices
      extends Registry[LWWMapKey[Connection, Location], LWWMap[Connection, Location]](
        Key = LWWMapKey(Constants.RegistryKey),
        EmptyValue = LWWMap.empty
      )

  /**
   * Service is a distributed registry which holds a location value against a connection-name.
   * At times, a location may not be available for a given connection-name, hence the location is an optional value.
   * It is used to track a single connection by subscribing events generated when associated location changes.
   *
   * @note Service has key as LWWRegisterKey[Option[Location]] as it represents the type of value that LWWRegister will hold.
   * But the value of LWWRegisterKey will still be connection-name.
   */
  class Service(connection: Connection)(implicit cluster: Cluster)
      extends Registry[LWWRegisterKey[Option[Location]], LWWRegister[Option[Location]]](
        Key = LWWRegisterKey(connection.name),
        EmptyValue = LWWRegister(Option.empty)
      )

}
