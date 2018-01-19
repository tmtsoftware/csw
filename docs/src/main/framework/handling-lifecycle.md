## Lifecycle support

### initialize

The `initialize` handler is invoked when the component is created. This is different than constructor initialization to allow non-blocking 
asynchronous operations. The component can initialize state such as configuration to be fetched from configuration service, 
location of components or services to be fetched from location service etc.

Assembly/Scala
:   @@snip [TromboneAssemblyHandlers.scala](../../../../csw-vslice/src/main/scala/csw/trombone/assembly/TromboneAssemblyHandlers.scala) { #initialize-handler }

Assembly/Java
:   @@snip [JTromboneAssemblyHandlers.java](../../../../csw-vslice/src/main/java/csw/trombone/assembly/JTromboneAssemblyHandlers.java) { #jInitialize-handler }

Hcd/Scala
:   @@snip [TromboneHcdHandlers.scala](../../../../csw-vslice/src/main/scala/csw/trombone/hcd/TromboneHcdHandlers.scala) { #initialize-handler }

Hcd/Java
:   @@snip [JTromboneHcdHandlers.java](../../../../csw-vslice/src/main/java/csw/trombone/hcd/JTromboneHcdHandlers.java) { #jInitialize-handler }

### onShutdown

The `onShutdown` handler can be used for carrying out the tasks which will allow the component to shutdown gracefully. 

### isOnline

A component has access to `isOnline` boolean flag which can be used to determine if the component is online or offline state.

### onGoOffline

A component can be notified to run in offline mode in case it is not in use. The component can change its behavior if needed as a part of this handler.

Assembly/Scala
:   @@snip [TromboneAssemblyHandlers.scala](../../../../csw-vslice/src/main/scala/csw/trombone/assembly/TromboneAssemblyHandlers.scala) { #onGoOffline-handler }

Assembly/Java
:   @@snip [JTromboneAssemblyHandlers.java](../../../../csw-vslice/src/main/java/csw/trombone/assembly/JTromboneAssemblyHandlers.java) { #onGoOffline-handler }

Hcd/Scala
:   @@snip [TromboneHcdHandlers.scala](../../../../csw-vslice/src/main/scala/csw/trombone/hcd/TromboneHcdHandlers.scala) { #onGoOffline-handler }

Hcd/Java
:   @@snip [JTromboneHcdHandlers.java](../../../../csw-vslice/src/main/java/csw/trombone/hcd/JTromboneHcdHandlers.java) { #onGoOffline-handler }

### onGoOnline

A component can be notified to run in online mode again in case it was put to run in offline mode. The component can change its behavior if needed as a part of this handler.

Assembly/Scala
:   @@snip [TromboneAssemblyHandlers.scala](../../../../csw-vslice/src/main/scala/csw/trombone/assembly/TromboneAssemblyHandlers.scala) { #onGoOnline-handler }

Assembly/Java
:   @@snip [JTromboneAssemblyHandlers.java](../../../../csw-vslice/src/main/java/csw/trombone/assembly/JTromboneAssemblyHandlers.java) { #onGoOnline-handler }

Hcd/Scala
:   @@snip [TromboneHcdHandlers.scala](../../../../csw-vslice/src/main/scala/csw/trombone/hcd/TromboneHcdHandlers.scala) { #onGoOnline-handler }

Hcd/Java
:   @@snip [JTromboneHcdHandlers.java](../../../../csw-vslice/src/main/java/csw/trombone/hcd/JTromboneHcdHandlers.java) { #onGoOnline-handler }

### Handling commands

#### validateCommand

A command can be sent as a `Submit` or `Oneway` message to the component actor. If a command can be completed immediately, a `CommandResponse` indicating 
the final response for the command can be returned. If a command requires time for processing, the component is required to validate the `ControlCommand` received
and return a validation result as `Accepted` or `Invalid`. The final response for a command sent as `Submit` can be obtained by the sender command by querying or
subscribing for this response to the component as described here. 

Assembly/Scala
:   @@snip [TromboneAssemblyHandlers.scala](../../../../csw-vslice/src/main/scala/csw/trombone/assembly/TromboneAssemblyHandlers.scala) { #validateCommand-handler }

Assembly/Java
:   @@snip [JTromboneAssemblyHandlers.java](../../../../csw-vslice/src/main/java/csw/trombone/assembly/JTromboneAssemblyHandlers.java) { #validateCommand-handler }

Hcd/Scala
:   @@snip [TromboneHcdHandlers.scala](../../../../csw-vslice/src/main/scala/csw/trombone/hcd/TromboneHcdHandlers.scala) { #validateCommand-handler }

Hcd/Java
:   @@snip [JTromboneHcdHandlers.java](../../../../csw-vslice/src/main/java/csw/trombone/hcd/JTromboneHcdHandlers.java) { #validateCommand-handler }
 
If a response can be provided immediately, a final `CommandResponse` such as `CommandCompleted` or `Error` can be sent from this handler.

#### onSubmit

On receiving a command as `Submit`, the `onSubmit` handler is invoked for a component only if the `validateCommand` handler returns `Accepted`. In case a command 
is received as a submit, command response should be updated in the `CommandResponseManager`. `CommandResponseManager` is an actor whose reference `commandResponseManager` 
is available in the `ComponentHandlers`. 

Assembly/Scala
:   @@snip [TromboneAssemblyHandlers.scala](../../../../csw-vslice/src/main/scala/csw/trombone/assembly/TromboneAssemblyHandlers.scala) { #onSubmit-handler }

Assembly/Java
:   @@snip [JTromboneAssemblyHandlers.java](../../../../csw-vslice/src/main/java/csw/trombone/assembly/JTromboneAssemblyHandlers.java) { #onSubmit-handler }

Hcd/Scala
:   @@snip [TromboneHcdHandlers.scala](../../../../csw-vslice/src/main/scala/csw/trombone/hcd/TromboneHcdHandlers.scala) { #onSubmit-handler }

Hcd/Java
:   @@snip [JTromboneHcdHandlers.java](../../../../csw-vslice/src/main/java/csw/trombone/hcd/JTromboneHcdHandlers.java) { #onSubmit-handler }

#### onOneway

On receiving a command as `Oneway`, the `onOneway` handler is invoked for a component only if the `validateCommand` handler returns `Accepted`.In case a command 
is received as a oneway, command response should not be provided to the sender.

Assembly/Scala
:   @@snip [TromboneAssemblyHandlers.scala](../../../../csw-vslice/src/main/scala/csw/trombone/assembly/TromboneAssemblyHandlers.scala) { #onOneway-handler }

Assembly/Java
:   @@snip [JTromboneAssemblyHandlers.java](../../../../csw-vslice/src/main/java/csw/trombone/assembly/JTromboneAssemblyHandlers.java) { #onOneway-handler }

Hcd/Scala
:   @@snip [TromboneHcdHandlers.scala](../../../../csw-vslice/src/main/scala/csw/trombone/hcd/TromboneHcdHandlers.scala) { #onOneway-handler }

Hcd/Java
:   @@snip [JTromboneHcdHandlers.java](../../../../csw-vslice/src/main/java/csw/trombone/hcd/JTromboneHcdHandlers.java) { #onOneway-handler }