package csw.param.models.params

import csw.param.TMTSerializable

final case class SerializableComponentInfo(
    name: String,
    componentType: String,
    prefix: String,
    behaviorFactoryClassName: String,
    locationServiceUsage: String,
    connections: String,
    initializeTimeout: String,
    runTimeout: String
) extends TMTSerializable
