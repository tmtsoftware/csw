## Tracking Connections

The component framework tracks the set of connections specified for a component in `ComponentInfo` if the `locationServiceUsage` property is set to `RegisterAndTrackServices`.
The framework also provides a helper `trackConnection` method to track any connection other than those present in `ComponentInfo`.

### onLocationTrackingEvent
The `onLocationTrackingEvent` handler can be used to take action on the `TrackingEvent` for a particular connection. This event could be for the connections in 
`ComponentInfo` tracked automatically or for the connections tracked explicitly using `trackConnection` method.

Assembly/Scala
:   @@snip [TromboneAssemblyHandlers.scala](../../../../csw-vslice/src/main/scala/csw/trombone/assembly/TromboneAssemblyHandlers.scala) { #onLocationTrackingEvent-handler }

Assembly/Java
:   @@snip [JTromboneAssemblyHandlers.java](../../../../csw-vslice/src/main/java/csw/trombone/assembly/JTromboneAssemblyHandlers.java) { #onLocationTrackingEvent-handler }

Hcd/Scala
:   @@snip [TromboneHcdHandlers.scala](../../../../csw-vslice/src/main/scala/csw/trombone/hcd/TromboneHcdHandlers.scala) { #onLocationTrackingEvent-handler }

Hcd/Java
:   @@snip [JTromboneHcdHandlers.java](../../../../csw-vslice/src/main/java/csw/trombone/hcd/JTromboneHcdHandlers.java) { #onLocationTrackingEvent-handler }