package csw.messages.models

import csw.messages.TMTSerializable

//TODO: what, why, how
case class Components(components: Set[Component]) extends TMTSerializable
