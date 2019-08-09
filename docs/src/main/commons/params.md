# Params

In the distributed environment of TMT observatory, **Components** communicate with each other 
by sending asynchronous **Messages**. These messages have a **Command** payload, which flows down 
through the Sequencer components to the Assemblies, HCDs and finally to the hardware. 
At each hop Commands are validated, interpreted and further propagated making their journey to its 
destination. Commands provide flexible placeholders to store values to convey precise intent of the sender component.

`csw-params` is available for Scala, Java, and [scala.js](https://www.scala-js.org/).

## Dependencies

sbt
:   @@@vars
    ```scala
    libraryDependencies += "com.github.tmtsoftware.csw" %% "csw-params" % "$version$"
    ```
    @@@


**csw-params** library provides out of the box support to cater to the diverse communication requirements. Consumer of this library will be able to create Commands, Events, States to store ParameterSets.

@@toc { depth=2 }

@@@ index
* [Keys and Parameters](../params/keys-parameters.md)
* [Units](../params/units.md)
* [Subsystem](../params/subsystem.md)
* [Commands](../params/commands.md)
* [Events](../params/events.md)
* [States](../params/states.md)
* [Result](../params/result.md)
@@@
