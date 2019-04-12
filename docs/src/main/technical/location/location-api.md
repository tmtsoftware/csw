# Location API

The @github[csw-location-api](/csw-location/csw-location-api) project provides a
@github[common API](/csw-location/csw-location-api/src/main/scala/csw/location/api/scaladsl/LocationService.scala) for the the `csw-location-server` and `csw-location-client` implementations.

This also includes the model classes used for connections, registration and component types as well as the
@github[TrackingEvent](/csw-location/csw-location-api/src/main/scala/csw/location/api/models/TrackingEvent.scala) class that clients receive whenever new location information is received.

## Java API

The Java API is defined partly in Scala code with traits like
@github[ILocationService](/csw-location/csw-location-api/src/main/scala/csw/location/api/javadsl/ILocationService.scala), and partly in Java, where classes like
@github[JComponentType](/csw-location/csw-location-api/src/main/java/csw/location/api/javadsl/JComponentType.java) are needed to provide easy access to Scala objects constants.
