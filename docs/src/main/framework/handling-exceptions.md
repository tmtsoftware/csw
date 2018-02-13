## Handling Exceptions

A component should create exceptions belonging to following two types:

1. **FailureRestart** : As a part of any handler, if an exception can be handled by restarting the component, an exception of type `FailureRestart` should be 
    thrown to let the framework restart the component. The component's state will be cleared/reinitialized. The `onInitialize` handler will be invoked again.
    
    Scala
    :   @@snip [AssemblyComponentHandlers.scala](../../../../examples/src/main/scala/csw/framework/components/assembly/AssemblyComponentHandlers.scala) { #failureRestart-Exception }
        
    Java
    :   @@snip [JTromboneAssemblyHandlers.java](../../../../csw-vslice/src/main/java/csw/trombone/assembly/JTromboneAssemblyHandlers.java) { #failureRestart-Exception }
        
2. **FailureStop** : As a part of any handler, an exception can be thrown of type `FailureStop` which will result in terminating the component. The `onShutdown` 
    handler will be invoked to facilitate graceful shutdown.
    
    Scala
    :   @@snip [AssemblyComponentHandlers.scala](../../../../examples/src/main/scala/csw/framework/components/assembly/AssemblyComponentHandlers.scala) { #failureStop-Exception }
    
    Java
    :   @@snip [JTromboneAssemblyHandlers.java](../../../../csw-vslice/src/main/java/csw/trombone/assembly/JTromboneAssemblyHandlers.java) { #failureStop-Exception }
