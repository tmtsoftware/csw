# Location API

The [csw-location-api]($github.dir.base_url$/csw-location/csw-location-api) project provides a
[common API]($github.base_url$/csw-location/csw-location-api/shared/src/main/scala/csw/location/api/scaladsl/LocationService.scala) for the the `csw-location-server` and `csw-location-client` implementations.

This also includes the model classes used for connections, registration and component types as well as the
[TrackingEvent]($github.base_url$/csw-location/csw-location-api/shared/src/main/scala/csw/location/api/models/TrackingEvent.scala) class that clients receive whenever new location information is received.

## Java API

The Java API is defined partly in Scala code with traits like
[ILocationService]($github.base_url$/csw-location/csw-location-api/jvm/src/main/scala/csw/location/api/javadsl/ILocationService.scala), and partly in Java, where classes like
[JComponentType]($github.base_url$/csw-location/csw-location-api/jvm/src/main/scala/csw/location/api/javadsl/JComponentType.scala) are needed to provide easy access to Scala objects constants.
