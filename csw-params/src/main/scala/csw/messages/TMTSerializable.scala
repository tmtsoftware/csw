package csw.messages

/**
 * Marker trait for serializing domain models over the wire.
 * This marker is configured to be serialized using Kryo.
 */
trait TMTSerializable extends Serializable
