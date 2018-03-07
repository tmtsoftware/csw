package csw.services

/**
 * == Command Service ==
 *
 * This project defines the basic classes and traits for the ''Command Service''.
 *
 * Related projects are:
 * - '''csw-messages''': This defines the types of command (Oneway/Submit etc.) and types of ''configurations'' (Setup/Observe/Wait etc.)
 * - '''framework''': This defines the Hcd and Assembly handlers, lifecycle manager and supervisor for components.
 *
 * Important classes in this project are:
 *
 * - [[csw.services.command.scaladsl.CommandResponseManager]]
 * This class wraps CommandResponseManagerActor and provides helpers to interact with actor
 * which is responsible for adding/updating/querying command result.
 * Component writers will get handle to CommandResponseManager in their handlers.
 * When component receives command of type [[csw.messages.CommandMessage.Submit]],
 * then framework (ComponentBehavior - TLA) will add a entry of this command with its validation status into CSRM (CommandResponseManager).
 *
 * In case of short running or immediate command,
 * validation response will be of type final result which can either be of type
 * [[csw.messages.ccs.commands.CommandResultType.Positive]] or [[csw.messages.ccs.commands.CommandResultType.Negative]]
 *
 * In case of long running command, validation response will be of type [[csw.messages.ccs.commands.CommandResultType.Intermediate]]
 * then it is the responsibility of component writer to update its final command status later on
 * with [[csw.messages.ccs.commands.CommandResponse]] which should be of type
 * [[csw.messages.ccs.commands.CommandResultType.Positive]] or [[csw.messages.ccs.commands.CommandResultType.Negative]]
 *
 * CommandResponseManager also provides **subscribe** API.
 * One of the use case for this is when Assembly splits top level command into two sub commands and forwards them to two different HCD's.
 * In this case, Assembly can register its interest in the final [[csw.messages.ccs.commands.CommandResponse]]
 * from two HCD's when these sub commands completes using **subscribe** API.
 * And once Assembly receives final command response from both the HCD's
 * then it can update Top level command with final [[csw.messages.ccs.commands.CommandResponse]]
 *
 *
 * - [[csw.services.command.scaladsl.CommandService]]
 * This class wraps the [[csw.messages.location.AkkaLocation]] and provides helpers to send commands to component actor extracted from provided location.
 * Normal component writers workflow would be to first resolve component using location service and then create CommandService instance using resolved location.
 *
 * Using this instance, you can Submit Command/Commands to other component or query for command result or subscribe for long running command result.
 *
 * === Example of CommandService Usage ===
 *
 * {{{
 *
 *    async{
 *       // here assemblyLocation is the resolved assembly location using LocationService
 *       val assemblyConfigService = new CommandService(assemblyLocation)
 *
 *       // here setupCommand is a ControlCommand which is created with prefix, command name, obs id and set of Parameters
 *       val initialResponse = await(assemblyConfigService.submit(setupCommand))
 *       // this can be a response of validation (Accepted, Invalid) or a final Response.
 *       // in case of response as `Accepted`, final CommandResponse can be obtained by using `subscribe` API.

 *       // suppose initialResponse is Accepted, then you can use subscribe API to obtain final response
 *       val finalResponse = await(assemblyConfigService.subscribe(setupCommand.runId))
 *    }
 *
 * }}}
 *
 * - [[csw.services.command.scaladsl.CommandDistributor]]
 * The ConfigDistributor enables distributing multiple commands to multiple components and get one aggregated command
 * response as a final response.
 *
 * === Example of CommandDistributor Usage ===
 *
 * {{{
 *
 *   // here hcdACommandService and hcdBCommandService are instances of CommandService created using HCD-A and HCD-B locations
 *   // setup1HcdA and setup2HcdA are Setup commands which are targeted to HCD-A
 *   // setup1HcdB and setup2HcdB are Setup commands which are targeted to HCD-B
 *   val aggregatedValidationResponse = CommandDistributor(
        Map(hcdACommandService → Set(setup1HcdA, setup2HcdA), hcdBCommandService → Set(setup1HcdB, setup2HcdB))
      ).aggregatedValidationResponse()
 *
 * }}}
 *
 */
package object command {}
