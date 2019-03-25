# CSW Framework Design Description

The *CSW Framework* provides the APIs used to talk to components,
such as HCDs and assemblies.
The framework, which is based on Akka [typed actors](https://doc.akka.io/docs/akka/current/typed/index.html), creates an actor for each component, along with a 
@github[supervisor](/csw-framework/src/main/scala/csw/framework/internal/supervisor/SupervisorBehavior.scala) 
and some related actors. 
For simplicity, component developers only need to implement a set of 
@ref:[handlers](../../framework/handling-lifecycle.md) 
(the
@github[ComponentHandlers](/csw-framework/src/main/scala/csw/framework/scaladsl/ComponentHandlers.scala) 
trait for Scala, the
@github[JComponentHandlers](/csw-framework/src/main/scala/csw/framework/javadsl/JComponentHandlers.scala)
interface for Java) which define how a component will behave.

See @ref:[here](../../commons/framework.md) for information on *using* the CSW framework to develop HCD and assembly components.

The @scaladoc[framework packge documentation](csw.framework/index) also contains an overview 
of the core classes that make up the CSW framework.

## Entry Points

In order to start an assembly or HCD component, you first need a 
@ref:[descriptor file](../../framework/describing-components.md)
(or object) called
@github[ComponentInfo](/csw-command/csw-command-client/src/main/scala/csw/command/client/models/framework/ComponentInfo.scala).

The name of the component info file can be passed as a command line argument to an application based on the 
@github[ContainerCmd](/csw-framework/src/main/scala/csw/framework/deploy/containercmd/ContainerCmd.scala)
class to
@ref:[deploy](../../framework/deploying-components.md)
the component.

In a production environment, it is planned that components will be started at boot time using a @ref:[HostConfig](../../apps/hostconfig.md) based application. 

## Component Creation

Components can be created in *standalone* mode (one component per JVM) or
*container* mode (multiple components running in a single JVM). 
When an HCD or assembly is
@ref:[created](../../framework/creating-components.md), depending on the mode,
either the
@github[ContainerBehaviorFactory](/csw-framework/src/main/scala/csw/framework/internal/container/ContainerBehaviorFactory.scala) or the 
@github[SupervisorBehaviorFactory](/csw-framework/src/main/scala/csw/framework/internal/supervisor/SupervisorBehaviorFactory.scala)
class is used to create the initial actor behavior. 
In container mode, a supervisor behavior is created for each component.

The supervisor then creates and watches the component and determines what to do when something goes wrong. 
It uses an instance of
@github[ComponentBehaviorFactory](/csw-framework/src/main/scala/csw/framework/scaladsl/ComponentBehaviorFactory.scala) 
that is created via reflection from an entry in the
 @ref:[component info file](../../framework/describing-components.md)
 file to create the component.

The top level actor (TLA) representing the 
@github[component's behavior](/csw-framework/src/main/scala/csw/framework/internal/component/ComponentBehavior.scala) uses instance of 
@github[ComponentHandlers](/csw-framework/src/main/scala/csw/framework/scaladsl/ComponentHandlers.scala)
returned from the supplied ComponentBehaviorFactory to handle incoming messages and commands for the component.





