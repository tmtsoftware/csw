package csw.messages.models

import csw.messages.TMTSerializable

case class Components(components: Set[Component]) extends TMTSerializable
