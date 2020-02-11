
Commands no longer contain create a runId when created (behind the scenes).
RunIds are created by the framework and passed into the handlers.
Result does not contain prefix

There is no more CompletedWithResult.
Result is returned in a Completed SubmitResponse.  If there is no result, an EmptyResult is returned.

Parameters
new jMadd for adding java.util.Set
