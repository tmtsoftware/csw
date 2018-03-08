package csw.messages.framework

import csw.messages.TMTSerializable

//TODO: what, why, how
case class Components(components: Set[Component]) extends TMTSerializable
