## Tracking Connections

The component framework tracks the set of connections specified for a component in `ComponentInfo` if the `locationServiceUsage` property is set to `RegisterAndTrackServices`.
The framework also provides a helper `trackConnection` method to track any connection other than those present in `ComponentInfo`.

### onLocationTrackingEvent
The `onLocationTrackingEvent` handler can be used to take action on the `TrackingEvent` for a particular connection. This event could be for the connections in 
`ComponentInfo` tracked automatically or for the connections tracked explicitly using `trackConnection` method.

Assembly/Scala
:   @@snip [AssemblyComponentHandlers.scala](../../../../examples/src/main/scala/csw/framework/components/assembly/AssemblyComponentHandlers.scala) { #onLocationTrackingEvent-handler }

Assembly/Java
:   @@snip [JAssemblyComponentHandlers.java](../../../../examples/src/main/java/csw/framework/components/assembly/JAssemblyComponentHandlers.java) { #onLocationTrackingEvent-handler }

Hcd/Scala
:   @@snip [HcdComponentHandlers.scala](../../../../examples/src/main/scala/csw/framework/components/hcd/HcdComponentHandlers.scala) { #onLocationTrackingEvent-handler }

Hcd/Java
:   @@snip [JHcdComponentHandlers.java](../../../../examples/src/main/java/csw/framework/components/hcd/JHcdComponentHandlers.java) { #onLocationTrackingEvent-handler }