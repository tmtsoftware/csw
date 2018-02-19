## Lifecycle support

### initialize

The `initialize` handler is invoked when the component is created. This is different than constructor initialization to allow non-blocking 
asynchronous operations. The component can initialize state such as configuration to be fetched from configuration service, 
location of components or services to be fetched from location service etc. These vary from component to component.

Assembly/Scala
:   @@snip [AssemblyComponentHandlers.scala](../../../../examples/src/main/scala/csw/framework/components/assembly/AssemblyComponentHandlers.scala) { #initialize-handler }

Assembly/Java
:   @@snip [JAssemblyComponentHandlers.java](../../../../examples/src/main/java/csw/framework/components/assembly/JAssemblyComponentHandlers.java) { #jInitialize-handler }

Hcd/Scala
:   @@snip [HcdComponentHandlers.scala](../../../../examples/src/main/scala/csw/framework/components/hcd/HcdComponentHandlers.scala) { #initialize-handler }

Hcd/Java
:   @@snip [JHcdComponentHandlers.java](../../../../examples/src/main/java/csw/framework/components/hcd/JHcdComponentHandlers.java) { #jInitialize-handler }

### onShutdown

The `onShutdown` handler can be used for carrying out the tasks which will allow the component to shutdown gracefully. 

Assembly/Scala
:   @@snip [AssemblyComponentHandlers.scala](../../../../examples/src/main/scala/csw/framework/components/assembly/AssemblyComponentHandlers.scala) { #onShutdown-handler }

Assembly/Java
:   @@snip [JAssemblyComponentHandlers.java](../../../../examples/src/main/java/csw/framework/components/assembly/JAssemblyComponentHandlers.java) { #onShutdown-handler }

Hcd/Scala
:   @@snip [HcdComponentHandlers.scala](../../../../examples/src/main/scala/csw/framework/components/hcd/HcdComponentHandlers.scala) { #onShutdown-handler }

Hcd/Java
:   @@snip [JHcdComponentHandlers.java](../../../../examples/src/main/java/csw/framework/components/hcd/JHcdComponentHandlers.java) { #onShutdown-handler }

### isOnline

A component has access to `isOnline` boolean flag which can be used to determine if the component is online or offline state.

### onGoOffline

A component can be notified to run in offline mode in case it is not in use. The component can change its behavior if needed as a part of this handler.

Assembly/Scala
:   @@snip [AssemblyComponentHandlers.scala](../../../../examples/src/main/scala/csw/framework/components/assembly/AssemblyComponentHandlers.scala) { #onGoOffline-handler }

Assembly/Java
:   @@snip [JAssemblyComponentHandlers.java](../../../../examples/src/main/java/csw/framework/components/assembly/JAssemblyComponentHandlers.java) { #onGoOffline-handler }

Hcd/Scala
:   @@snip [HcdComponentHandlers.scala](../../../../examples/src/main/scala/csw/framework/components/hcd/HcdComponentHandlers.scala) { #onGoOffline-handler }

Hcd/Java
:   @@snip [JHcdComponentHandlers.java](../../../../examples/src/main/java/csw/framework/components/hcd/JHcdComponentHandlers.java) { #onGoOffline-handler }

### onGoOnline

A component can be notified to run in online mode again in case it was put to run in offline mode. The component can change its behavior if needed as a part of this handler.

Assembly/Scala
:   @@snip [AssemblyComponentHandlers.scala](../../../../examples/src/main/scala/csw/framework/components/assembly/AssemblyComponentHandlers.scala) { #onGoOnline-handler }

Assembly/Java
:   @@snip [JAssemblyComponentHandlers.java](../../../../examples/src/main/java/csw/framework/components/assembly/JAssemblyComponentHandlers.java) { #onGoOnline-handler }

Hcd/Scala
:   @@snip [HcdComponentHandlers.scala](../../../../examples/src/main/scala/csw/framework/components/hcd/HcdComponentHandlers.scala) { #onGoOnline-handler }

Hcd/Java
:   @@snip [JHcdComponentHandlers.java](../../../../examples/src/main/java/csw/framework/components/hcd/JHcdComponentHandlers.java) { #onGoOnline-handler }

### Handling commands

#### validateCommand

A command can be sent as a `Submit` or `Oneway` message to the component actor. If a command can be completed immediately, a `CommandResponse` indicating 
the final response for the command can be returned. If a command requires time for processing, the component is required to validate the `ControlCommand` received
and return a validation result as `Accepted` or `Invalid`. The final response for a command sent as `Submit` can be obtained by the sender command by querying or
subscribing for this response to the component as described here. 

Assembly/Scala
:   @@snip [AssemblyComponentHandlers.scala](../../../../examples/src/main/scala/csw/framework/components/assembly/AssemblyComponentHandlers.scala) { #validateCommand-handler }

Assembly/Java
:   @@snip [JAssemblyComponentHandlers.java](../../../../examples/src/main/java/csw/framework/components/assembly/JAssemblyComponentHandlers.java) { #validateCommand-handler }

Hcd/Scala
:   @@snip [HcdComponentHandlers.scala](../../../../examples/src/main/scala/csw/framework/components/hcd/HcdComponentHandlers.scala) { #validateCommand-handler }

Hcd/Java
:   @@snip [JHcdComponentHandlers.java](../../../../examples/src/main/java/csw/framework/components/hcd/JHcdComponentHandlers.java) { #validateCommand-handler }
 
If a response can be provided immediately, a final `CommandResponse` such as `CommandCompleted` or `Error` can be sent from this handler.

#### onSubmit

On receiving a command as `Submit`, the `onSubmit` handler is invoked for a component only if the `validateCommand` handler returns `Accepted`. In case a command 
is received as a submit, command response should be updated in the `CommandResponseManager`. `CommandResponseManager` is an actor whose reference `commandResponseManager` 
is available in the `ComponentHandlers`. 

Assembly/Scala
:   @@snip [AssemblyComponentHandlers.scala](../../../../examples/src/main/scala/csw/framework/components/assembly/AssemblyComponentHandlers.scala) { #onSubmit-handler }

Assembly/Java
:   @@snip [JAssemblyComponentHandlers.java](../../../../examples/src/main/java/csw/framework/components/assembly/JAssemblyComponentHandlers.java) { #onSubmit-handler }

Hcd/Scala
:   @@snip [HcdComponentHandlers.scala](../../../../examples/src/main/scala/csw/framework/components/hcd/HcdComponentHandlers.scala) { #onSubmit-handler }

Hcd/Java
:   @@snip [JHcdComponentHandlers.java](../../../../examples/src/main/java/csw/framework/components/hcd/JHcdComponentHandlers.java) { #onSubmit-handler }

#### onOneway

On receiving a command as `Oneway`, the `onOneway` handler is invoked for a component only if the `validateCommand` handler returns `Accepted`.In case a command 
is received as a oneway, command response should not be provided to the sender.

Assembly/Scala
:   @@snip [AssemblyComponentHandlers.scala](../../../../examples/src/main/scala/csw/framework/components/assembly/AssemblyComponentHandlers.scala) { #onOneway-handler }

Assembly/Java
:   @@snip [JAssemblyComponentHandlers.java](../../../../examples/src/main/java/csw/framework/components/assembly/JAssemblyComponentHandlers.java) { #onOneway-handler }

Hcd/Scala
:   @@snip [HcdComponentHandlers.scala](../../../../examples/src/main/scala/csw/framework/components/hcd/HcdComponentHandlers.scala) { #onOneway-handler }

Hcd/Java
:   @@snip [JHcdComponentHandlers.java](../../../../examples/src/main/java/csw/framework/components/hcd/JHcdComponentHandlers.java) { #onOneway-handler }
