package csw.param.models

final case class SerializableComponentInfo(
    name: String,
    componentType: String,
    prefix: String,
    behaviorFactoryClassName: String,
    locationServiceUsage: String,
    connections: String,
    initializeTimeout: String,
    runTimeout: String
)
