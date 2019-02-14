# Framework for creating components (HCD, Assembly, Container)

**csw-framework** library provides support for creating a component as defined by the TMT. 

## Dependencies

sbt
:   @@@vars
    ```scala
    libraryDependencies += "com.github.tmtsoftware.csw" %% "csw-framework" % "$version$"
    ```
    @@@
    
@@toc { depth=2 } 
    
@@@index
* [Describing Components](../framework/describing-components.md)
* [Creating Components](../framework/creating-components.md)
* [Implementing Handlers](../framework/handling-lifecycle.md)
* [Managing Command State](../framework/managing-command-state.md)
* [Tracking Connections](../framework/tracking-connections.md)
* [Publishing State](../framework/publishing-state.md)
* [Handling Exceptions](../framework/handling-exceptions.md)
* [Deployment](../framework/deploying-components.md)
@@@

## Technical Description
See @ref:[CSW Framework Technical Description](../technical/framework/framework.md).

## Source code for examples

* @github[Assembly Scala Example](/examples/src/main/scala/example/framework/components/assembly/AssemblyComponentHandlers.scala)
* @github[HCD Scala Example](/examples/src/main/scala/example/framework/components/hcd/HcdComponentHandlers.scala)
* @github[Assembly Java Example](/examples/src/main/java/example/framework/components/assembly/JAssemblyComponentHandlers.java)
* @github[HCD Java Example](/examples/src/main/java/example/framework/components/hcd/JHcdComponentHandlers.java)
    



