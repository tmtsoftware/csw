# Messages

In the distributed environment of TMT observatory, **Components** communicate with each other 
by sending asynchronous **Messages**. These messages have a **Command** payload, which flows down 
through the Sequencer components to the Assemblies, HCDs and finally to the hardware. 
At each hop Commands are validated, interpreted and further propagated making their journey to its 
destination. Commands provide flexible placeholders to store values to convey precise intent of the sender component.

## Dependencies

sbt
:   @@@vars
    ```scala
    libraryDependencies += "org.tmt" %% "csw-messages" % "$version$"
    ```
    @@@

**csw-messages** library provides out of the box support to cater to the diverse communication requirements. Consumer of this library will be able to create Commands, Events, States to store ParameterSets.

@@toc { depth=2 }

@@@ index
* [Keys and Parameters](../messages/keys-parameters.md)
* [Units](../messages/units.md)
* [Subsystem](../messages/subsystem.md)
* [Commands](../messages/commands.md)
* [Events](../messages/events.md)
* [States](../messages/states.md)
* [Result](../messages/result.md)
@@@
