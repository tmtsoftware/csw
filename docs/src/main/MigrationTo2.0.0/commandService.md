# Command Service in CSW 2

The Command Service got a rework in CSW 2 to make it more intuitive.  One of the major differences is that the `runId`
of a command is no longer created in the client code when creating a Setup or Observe.  It is instead created by the 
framework when receiving a command, and then passed to the command handlers as an argument.  Therefore `validateCommand`,
`onSubmit`, and `onOneway` handlers all now take a `runId` in addition to the command.

When a command is received via a `Submit`, the `runId` is created and passed with the command to the `validateCommand` behavior.
If the command is `Accepted`, the `runId` and command are passed to the `onSubmit` handler.  For long running commands,
a `Started` message is returned containing the `runId`.  This is then registered with `CommandResponseManager`, which 
has also been streamlined in this release.  The `Started` response is then returned to the sender of the command, which
can use the included `runId` to query the command for a final response.  

The `query` command in CSW 1 would return a `QueryResponse`, which would essentially be any `SubmitResponse` plus an additional
response for the case when the Command `runId` being queried is not registered in the CRM.  For CSW 2, this case now returns
an `InvalidResponse` with a `IdNotAvailableIssue`, thus eliminating the need for a `QueryResponse`.

For final responses, the `CompletedWithResult` response type has been removed.  Instead, all `Completed` responses contain
a `Result` with it.  If the command does not return a result, this `result` value will be an `EmptyResult` type.

A default timeout of 5 seconds has been added to all commands.  This timeout can be overridden in `submitAndWait` and 
`queryFinal` calls in Scala. For Java, the timeout is a mandatory argument to these calls.

As mentioned before, the CRM has been streamlined, but most improvements are internal and not visible to the developer. 
One API change is that the `updateSubCommand` method is no longer supported.

Handlers have also been added for `onDiagnosticMode` and `onOperationsMode`. The corresponding commands have been 
added to the `CommandService` object created when using a `CommandServiceFactory` and the component's location.

Additionally, commands can now be sent to Assemblies and HCDs using HTTP.  A HTTP version of the `CommandService` can be
obtained from the `CommandServiceFactory` using an `HTTPLocation`.  If an `AkkaLocation` is used, the normal Akka-based
`CommandService` is obtained.

