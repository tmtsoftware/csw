package csw.services

/**
 * == Command Service ==
 *
 * This project defines the basic classes and traits for the ''Command Service''.
 *
 * Related projects are:
 * - '''csw-messages''':
 *   - This defines the types of command (Oneway/Submit etc.) and types of ''configurations'' (Setup/Observe/Wait etc.)
 *   - Complete usage of Messages is available at: https://tmtsoftware.github.io/csw-prod/services/messages.html
 *
 * - '''framework''':
 *   - This defines the Hcd and Assembly handlers, lifecycle manager and supervisor for components.
 *   - Framework allows component writer to override onValidation, onSubmit and onOneway handlers. (Note it allows overriding other handlers as well.)
 *   - On every command received by component, onValidation handler gets invoked where received command gets validated and validation response is returned.
 *   - Based on validation response and command type, onSubmit/onOneway hooks gets invoked where command gets processed.
 *   - Complete details of handling commands can be found here : https://tmtsoftware.github.io/csw-prod/framework/handling-lifecycle.html#handling-commands
 *
 * Important classes in this project are:
 *
 * - [[csw.services.command.CommandResponseManager]]
 *
 * This class wraps CommandResponseManagerActor and provides helpers to interact with actor which is responsible for adding/updating/querying command result.
 * Component writers will get handle to CommandResponseManager in their handlers.
 *
 * - [[csw.services.command.internal.CommandResponseManagerBehavior]] maintains two states:
 *  - [[csw.messages.commands.CommandResponseManagerState]]:
 *      It maintains [[csw.messages.params.models.Id]] of Commands and their corresponding [[csw.messages.commands.CommandState]].
 *  - [[csw.messages.commands.CommandCorrelation]] :
 *      It maintains commands [[csw.messages.params.models.Id]] correlation between parent to child and child to parent.
 *
 * - [[csw.services.command.scaladsl.CommandService]]
 *
 * This class wraps the [[csw.messages.location.AkkaLocation]] and provides helpers to send commands to component actor extracted from provided location.
 * Normal component writers workflow would be to first resolve component using location service and then create CommandService instance using resolved location.
 *
 * Using this instance, you can Submit Command/Commands to other component or query for command result or subscribe for long running command result.
 *
 * - [[csw.services.command.scaladsl.CommandDistributor]]
 *
 * When you have multiple commands targeted to multiple components then you can use ConfigDistributor.
 * Using CommandDistributor utility you can send all these commands in one go and get aggregated response.
 *
 * Complete guide of usage of different API's provided by CommandService is available at:
 * https://tmtsoftware.github.io/csw-prod/command.html
 *
 */
package object command {}
