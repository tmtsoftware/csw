# Migration Guide from 3.0.0 to 4.0.0

This guide focuses on changes that may require current code to be changed to go from 
CSW Version 3.0.0 to CSW Version 4.0.0. From the release notes, the following are
the important changes from 3.0.0 that may require code changes:

- Removed `allTags` and `allTagNames` method from `Coords` and `JCoords`

- `frameworkWiring` instance from `FrameworkTestKit` is marked private. You need to change your imports from
  `import frameworkTestKit.frameworkWiring._` to `import frameworkTestKit._`. This import also brings in implicit
  actorSystem in scope. If you are explicitly creating implicit actorSystem in your test, then you can safely remove
  that and use one imported via `FrameworkTestKit`. Note that Java users are not impacted with this change.

- An existing TAITime or UTCTime parameter should add a unit as `tai` or `utc`, respectively.
Currently, time parameters have no units.

- The `RaDec` parameter was a test parameter was retired and replaced with a `Coord` parameters. As far as 
we know, no one was using the RaDec parameter.

- The current `struct` parameter has been removed. This removal was announced with version 3.0. A new
struct parameter hoped for and is on the TODO list, but a design does not exist, and work is not scheduled.

- The subsystem list has been updated to track the changes in the Systems Engineering N2 diagram. This
change should not impact your code, based on our understanding of who is developing code.
