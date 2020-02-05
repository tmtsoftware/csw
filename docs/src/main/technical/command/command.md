# Command

Commands can be sent to other component and responses can be received in return. To understand the underlying framework 
of the components and its deployment, please refer to the @ref[framework technical doc](../framework/framework.md).

## Sending Commands from the Component

The types of commands that can be sent by a component are discussed @ref[here](../../commons/create-component.md#receiving-commands). In order to send
commands to other components, a [CommandService]($github.base_url$/csw-command/csw-command-api/shared/src/main/scala/csw/command/api/scaladsl/CommandService.scala) helper
is needed. `CommandService` helper is used to send commands to a component in the form of methods instead of sending messages directly to a component's
Supervisor actor. The creation of a CommandService instance can be found @ref[here](../../commons/multiple-components.md#sending-commands).

The operations allowed through `CommandService` helper are as follows:

- [validate]($github.base_url$/csw-command/csw-command-client/src/main/scala/csw/command/client/internal/CommandServiceImpl.scala#L38)
- [submit]($github.base_url$/csw-command/csw-command-client/src/main/scala/csw/command/client/internal/CommandServiceImpl.scala#L46)
- [submitAndWait]($github.base_url$/csw-command/csw-command-client/src/main/scala/csw/command/client/internal/CommandServiceImpl.scala#L49)
- [submitAllAndWait]($github.base_url$/csw-command/csw-command-client/src/main/scala/csw/command/client/internal/CommandServiceImpl.scala#L55)
- [oneway]($github.base_url$/csw-command/csw-command-client/src/main/scala/csw/command/client/internal/CommandServiceImpl.scala#L74)
- [onewayAndMatch]($github.base_url$/csw-command/csw-command-client/src/main/scala/csw/command/client/internal/CommandServiceImpl.scala#L77)
- [query]($github.base_url$/csw-command/csw-command-client/src/main/scala/csw/command/client/internal/CommandServiceImpl.scala#L96)
- [queryFinal]($github.base_url$/csw-command/csw-command-client/src/main/scala/csw/command/client/internal/CommandServiceImpl.scala#L104)
- [subscribeCurrentState]($github.base_url$/csw-command/csw-command-client/src/main/scala/csw/command/client/internal/CommandServiceImpl.scala#L100)
 
## Receiving Responses from Components

### Submit

To understand the flow of the Submit command, please refer to this @ref[section](../../commons/command.md#the-submit-message). 

![submit](media/submit.png)

### Oneway

To understand the flow of Oneway, please refer to this @ref[section](../../commons/command.md#the-oneway-message).
 
![oneway](media/oneway.png)

### Validate

To understand the flow of the Validate command, please refer to this @ref[section](../../commons/command.md#validate) and the code base for the implementation can be
found [here]($github.base_url$/csw-framework/src/main/scala/csw/framework/internal/component/ComponentBehavior.scala#L154).

### Command Response Manager

Each component contains a Command Response Manager (CRM) with the sole purpose of helping to manage responses for long-running comamnds.
If an Assembly sends a command to an HCD that is long-running, the HCD returns the `Started` response. The framework in the HCD notices the
`Started` response and makes an entry in its CRM that a sender actor is may be expecting a final response for the command with the associated
runId. It also tracks the most recent `SubmitResponse` for the command, which will always be `Started` when entered into the CRM.

If the Assembly (or any component) issues a `query`, the message is passed by the HCD's Supervisor to the CRM, which returns the most recent
response associated with the runId. If the Assembly (or any component) issues a `queryFinal` the HCD's Supervisor forwards the request to the
CRM, which makes an entry to remmber that there is an Actor that needs to be updated with the final response for the runId. If the current
response is already a final response, the Actor is updated immediately and no entry is made in the CRM.

When the actions in the HCD complete, the HCD uses the `updateCommand` method of the CRM.  WHen this happens, the CRM updates the
most recent response for the runId and then checks it's table to
determine if there are any Actors waiting for the runId's final response. If there are waiting Actors, each of them is sent the final response, 
and each is then removed from the CRM tables.  If there are no waiting Actors, only the current response for the runId is updated ready for the
possibiity that there will be a future `query` or `queryFinal`.

The CRM remembers roughly 10 recent commands, so `query` and `queryFinal` may be used successfully with commands that have completed for some
amount of time. If there is no entry in the CRM for a provided runId, the CRM returns an `Error` response indicating it does not know the runId.
 
The CRM also provides a utility method called `queryFinalAll`. This is just a wrapper around the Future.sequence call that allows components sending
sub-commands to wait for all the sub-commands to complete.

The Assembly worker can communicate with `CommandResponseManagerActor` using [CommandResponseManager]($github.base_url$/csw-command/csw-command-client/src/main/scala/csw/command/client/CommandResponseManager.scala)
coming via [CswContext]($github.base_url$/csw-framework/src/main/scala/csw/framework/models/CswContext.scala#L43).

### Current State Pub/Sub Functionality

The framework provides a way, based only on Akka, for one component to subscribe to `CurrentState` events provided in another component.
This can be used by an HCD to keep an Assembly up to date on its internal state asynchronously outside of commands. This can also be coupled with the use of 
`Oneway` commands that do not provide completion responses to the sender as is done for `Submit`. 
The provider of CurrentState can use [CurrentStatePublisher]($github.base_url$/csw-framework/src/main/scala/csw/framework/models/CswContext.scala#L42)
to publish its state from `CswContext` and the sender component can receive state using [subscribeCurrentState]($github.base_url$/csw-command/csw-command-client/src/main/scala/csw/command/client/internal/CommandServiceImpl.scala#L100)
from `CommandService`.

The Current State Pub/Sub is implemented in [PubSubBehavior]($github.base_url$/csw-framework/src/main/scala/csw/framework/internal/pubsub/PubSubBehavior.scala)