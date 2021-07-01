# Migration Guide from 3.0.0 to 4.0.0

This guide shows how to migrate from CSW Version 3.0.0 to CSW Version 4.0.0. From the release notes, the following
are the important changes from 3.0.0:

- Added `JDefaultComponentHandlers` and `DefaultComponentHandlers` - 
  default component handlers to create assembly or hcd in both scala and java.
  Details on how to use these handlers are @ref[here](../../commons/unit-tests.md#spawning-a-component-using-defaultcomponenthandlers) 
  
- Removed `allTags` and `allTagNames` method from `Coords` and `JCoords`
