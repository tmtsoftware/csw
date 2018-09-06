package csw.messages.framework

import csw.messages.TMTSerializable

/**
 * Represents a collection of components created in a single container
 *
 * @param components a set of components with its supervisor and componentInfo
 */
case class Components(components: Set[Component]) extends TMTSerializable
