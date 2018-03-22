# Creating a Component

TODO This page should at least mention all of the features of CSW a developer might want to use,
but only give details on the most commonly used.  Links/references should be provided for 
the other features.

This gives a walkthrough of creating an HCD. 

This tutorial helps in creating a HCD in Scala/Java. In order to create a HCD one needs to depend on `csw-framework`
which can be referred [here](https://tmtsoftware.github.io/csw-prod/framework.html). This tutorial can be referred for creating an Assembly as well.

## Anatomy of Component
    
A component consists of a supervisor actor, a top level actor, a component handler and one or more worker actors. From all these, `csw-framework`
provides supervisor actor, a top level actor and abstract class of handlers. Component developers are expected to implement this handler which also
acts as a gateway from framework to component code.   
     
### Supervisor

A Supervisor actor is the actor first started for any component. Two main responsibilities that supervisor performs is as follows:

-   Spawn a top level actor for the component and start watching it
-   Register itself with location service

@@@ note { title=Note }

Supervisor registers itself with location service. That means supervisor acts as a gateway for external component/entity to talk to this component.

@@@

The source code of supervisor actor can be referred [here](https://github.com/tmtsoftware/csw-prod/blob/master/csw-framework/src/main/scala/csw/framework/internal/supervisor/SupervisorBehavior.scala)

### Top level actor

A top level actor is started by supervisor actor for any component. It takes a handler implementation (an instance of ComponentHandlers) as constructor parameter.
The handler implementation would be written by component developer.

Whenever a message is received by top level actor, it calls an appropriate method of handlers which we refer as hooks in further explanation. For e.g.
if top level actor receives `Initialize` message it will call `initialize()` hook of handlers.

The source code of top level actor can be referred [here](https://github.com/tmtsoftware/csw-prod/blob/master/csw-framework/src/main/scala/csw/framework/internal/component/ComponentBehavior.scala).

### Handlers

A `ComponentHandlers` is an abstract class provided by `csw-framework`. It provides a list of methods that a component developer should implement:

-   initialize
-   validateCommand
-   onSubmit
-   onOneway
-   onGoOffline
-   onGoOnline
-   onLocationTrackingEvent
-   trackConnection
-   onShutdown

The source code of `ComponentHandlers` can be referred [here](https://github.com/tmtsoftware/csw-prod/blob/master/csw-framework/src/main/scala/csw/framework/scaladsl/ComponentHandlers.scala). 

More details about handler significance and invocation can be referred [here](https://tmtsoftware.github.io/csw-prod/framework/handling-lifecycle.html)

@@@ note { title=Note }

If the component developer wishes to write the handler implementation as java code, then he/she needs to implement the java version of `ComponentHandlers`
which is `JComponentHandlers`. The source code of `JComponentHandlers` can be referred [here](https://github.com/tmtsoftware/csw-prod/blob/master/csw-framework/src/main/scala/csw/framework/javadsl/JComponentHandlers.scala). 

@@@

## Constructing the Component

After writing the handlers, component developer needs to wire them up with framework. In order to do this, developer 
needs to implement a `ComponentBehaviorFactory`. This factory should to be configured in configuration file for
the component. The sample of configuration file is discussed next. The `csw-framework` then picks up the full path of
`ComponentBehaviorFactory` from configuration file and spawn the component handlers using this factory as a process of
booting a component. The factory is instantiated using java reflection.

The sample code to implement the `ComponentBehaviorFactory` can be referred [here](https://tmtsoftware.github.io/csw-prod/framework/creating-components.html) 

### Component Configuration (ComponentInfo)

Component configuration contains details needed to spawn a component. This configuration resides in a configuration file
for a particular component. The sample for a HCD is as follows:

```
{
    name = "Motion_Controller"
    componentType = hcd
    behaviorFactoryClassName = tcs.hcd.SampleComponentBehaviorFactory
    prefix = tcs.mobie.blue.filter
    locationServiceUsage = RegisterOnly
}
``` 

@@@ note { title=Note }

`behaviorFactoryClassName` refers to class name of the concrete implementation of `ComponentBehaviorFactory`, which is `SampleComponentBehaviorFactory` in above sample.

@@@

The `name` and `componentType` is used to create the `ComponentId` representing a unique component in location service.

The `locationServiceUsage` is referred by supervisor actor to decide whether to only register a component with location service or register and track other components.
  
The configuration file is parsed to `ComponentInfo` and injected in supervisor actor. It is then injected in `ComponentHandlers` while spawning a component.

More details about `ComponentInfo` can be referred [here](https://tmtsoftware.github.io/csw-prod/framework/describing-components.html).

A sample configuration file can be referred [here](https://github.com/tmtsoftware/csw-prod/blob/master/csw-benchmark/src/main/resources/container.conf).

## Lifecycle 

Describe lifecycle states, transitions, etc.
- Registration in Location Service (other Location Service details are in the next part)
- Loading a Configuration from Config Service (Again, details are in next part)

## Logging

## Receiving Commands

### Command types, and Implementing Handler stubs
### Validation
### CSRM

## Building, Staging, PublishLocal 

Just for this in Standlone mode, but link to reference

## Running Component (Standalone mode)

