package csw.command

/**
 * == Command Service ==
 *
 * This project defines the basic classes and traits for the ''Command Service''.
 *
 * Related projects are:
 * - '''csw-messages''':
 *   - This defines the types of command (Oneway/Submit etc.) and types of ''configurations'' (Setup/Observe/Wait etc.)
 *   - Complete usage of Messages is available at: https://tmtsoftware.github.io/csw/services/messages.html
 *
 * - '''framework''':
 *   - This defines the Hcd and Assembly handlers, lifecycle manager and supervisor for components.
 *   - Framework allows component writer to override onValidation, onSubmit and onOneway handlers. (Note it allows overriding other handlers as well.)
 *   - On every command received by component, onValidation handler gets invoked where received command gets validated and validation response is returned.
 *   - Based on validation response and command type, onSubmit/onOneway hooks gets invoked where command gets processed.
 *   - Complete details of handling commands can be found here : https://tmtsoftware.github.io/csw/framework/handling-lifecycle.html#handling-commands
 *
 * Important classes in this project are:
 *
 * - [[csw.command.client.CommandResponseManager]]
 *
 * This class wraps CommandResponseManagerActor and provides helpers to interact with actor which is responsible for adding/updating/querying command result.
 * Component writers will get handle to CommandResponseManager in their handlers.
 *
 * - [[csw.command.client.internal.CommandResponseManagerBehavior]] maintains two states:
 *  - [[csw.command.client.internal.CommandResponseState]]:
 *      It maintains [[csw.params.core.models.Id]] of Commands and their corresponding [[csw.command.client.internal.CommandState]].
 *  - [[csw.command.client.internal.CommandCorrelation]] :
 *      It maintains commands [[csw.params.core.models.Id]] correlation between parent to child and child to parent.
 *
 * - [[csw.command.api.scaladsl.CommandService]]
 *
 * This class wraps the [[csw.location.api.models.AkkaLocation]] and provides helpers to send commands to component actor extracted from provided location.
 * Normal component writers workflow would be to first resolve component using location service and then create CommandService instance using resolved location.
 *
 * Using this instance, you can Submit Command/Commands to other component or query for command result or subscribe for long running command result.
 *
 * When you have multiple commands targeted to multiple components then you can use ConfigDistributor.
 * Using CommandDistributor utility you can send all these commands in one go and get aggregated response.
 *
 * Complete guide of usage of different API's provided by CommandService is available at:
 * https://tmtsoftware.github.io/csw/command.html
 *
 */
package object client {}
