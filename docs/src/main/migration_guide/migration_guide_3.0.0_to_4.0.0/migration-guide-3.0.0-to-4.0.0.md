# Migration Guide from 3.0.0 to 4.0.0

This guide shows how to migrate from CSW Version 3.0.0 to CSW Version 4.0.0. From the release notes, the following are
the important changes from 3.0.0:

- Added `JDefaultComponentHandlers` and `DefaultComponentHandlers` - default component handlers to create assembly or
  hcd in both scala and java. Details on how to use these handlers are
  @ref[here](../../commons/unit-tests.md#spawning-a-component-using-defaultcomponenthandlers)

- Removed `allTags` and `allTagNames` method from `Coords` and `JCoords`

- `frameworkWiring` instance from `FrameworkTestKit` is marked private. You need to change your imports from
  `import frameworkTestKit.frameworkWiring._` to `import frameworkTestKit._`. This import also brings in implicit
  actorSystem in scope. If you are explicitly creating implicit actorSystem in your test, then you can safely remove
  that and use one imported via `FrameworkTestKit`. Note that Java users are not impacted with this change.
