package csw.services.location.models

/**
  * All models that resides in CRDT has to be serializable using TmtSerializable. It is required since they have to be communicable over the network.
  */
trait TmtSerializable extends Serializable
