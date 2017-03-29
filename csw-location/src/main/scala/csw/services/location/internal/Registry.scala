package csw.services.location.internal

import akka.cluster.Cluster
import akka.cluster.ddata.Replicator._
import akka.cluster.ddata._
import csw.services.location.models.{Connection, Location}

import scala.concurrent.duration.DurationDouble

/**
  * A `Registry` that manages [[akka.cluster.ddata.Replicator.Update]] and [[akka.cluster.ddata.Replicator.Get]] messages
  * for both cluster data types namely, [[akka.cluster.ddata.LWWRegister]] and [[akka.cluster.ddata.LWWMap]]
  *
  * @param Key        The `Key` against which `update` or `get` will be performed
  * @param EmptyValue The initial value passed to [[akka.cluster.ddata.Replicator.Update]]
  * @tparam K The type of `Key`
  * @tparam V The type of `ReplicatedData`
  */
class Registry[K <: Key[V], V <: ReplicatedData](val Key: K, val EmptyValue: V) {
  /**
    * Creates [[akka.cluster.ddata.Replicator.Update]] with the `Key`, `EmptyValue` and function `f` which will update
    * majority nodes in cluster
    *
    * @param f A callback function which is passed to Update
    */
  def update(f: V ⇒ V): Update[V] = Update(Key, EmptyValue, WriteMajority(5.seconds))(f)

  /**
    * Creates [[akka.cluster.ddata.Replicator.Get]] with `Key` which will read from majority nodes in cluster
    */
  def get: Get[V] = Get(Key, ReadLocal)
}

object Registry {

  /**
    * Manages a `LWWMap` of `Connection` to `Location`. This data will be mainly used to list all locations registered in
    * cluster
    */
  object AllServices extends Registry[LWWMapKey[Connection, Location], LWWMap[Connection, Location]](
    Key = LWWMapKey(Constants.RegistryKey),
    EmptyValue = LWWMap.empty
  )

  /**
    * Manages a `LWWRegister` with `Connection` name as `Key` and `Option`[`Location`] as value. This data will be mainly
    * used to subscribe [[akka.cluster.ddata.Replicator.Changed]] messages for `Key` in `LWWRegister` and provide equivalent
    * [[csw.services.location.models.TrackingEvent]] to components. On delete the `Key` will never be removed from `LWWRegister`
    * but the value will be updated to `None`
    *
    * @param connection The name of the connection which is unique across the cluster will be used as `Key`
    * @param cluster    The cluster is used to create `LWWRegister`
    */
  class Service(connection: Connection)(implicit cluster: Cluster) extends Registry[LWWRegisterKey[Option[Location]], LWWRegister[Option[Location]]](
    Key = LWWRegisterKey(connection.name),
    EmptyValue = LWWRegister(Option.empty)
  )

}
