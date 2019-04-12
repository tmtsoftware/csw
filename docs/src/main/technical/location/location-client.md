# CSW Location Client

The @github[csw-location-client](/csw-location/csw-location-client) project provides a convenient lightweight wrapper for accessing the CSW Location HTTP Server.

The client API implements the same @github[LocationService](/csw-location/csw-location-api/src/main/scala/csw/location/api/scaladsl/LocationService.scala) trait as the server API.
The core implementation is in the @github[LocationServiceClient](/csw-location/csw-location-client/src/main/scala/csw/location/client/internal/LocationServiceClient.scala) class, which is private, but can be instantiated via the @github[HttpLocationServiceFactory](/csw-location/csw-location-client/src/main/scala/csw/location/client/scaladsl/HttpLocationServiceFactory.scala)
class.

## Java API

The Java location client API is implemented in Scala as a thin wrapper class:
@github[JHttpLocationServiceFactory](/csw-location/csw-location-client/src/main/scala/csw/location/client/javadsl/JHttpLocationServiceFactory.scala).
It delegates to the private class
@github[JLocationServiceImpl](/csw-location/csw-location-client/src/main/scala/csw/location/client/internal/JLocationServiceImpl.scala),
which converts the returned `Future` and `Option` types to their Java equivalent.

## Tests

There are only a few tests in the `csw-location-client` project. Most of the features are actually tested in the `csw-location-server` project.
