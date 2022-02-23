# Migration Guide from 4.0.0 to 5.0.0

This guide focuses on changes that may require current code to be changed to go from 
CSW Version 4.0.0 to CSW Version 5.0.0. From the release notes, the following are
the important changes from 4.0.0 that may require code changes:

`ComponentInfo` that is being provided via `standalone.conf`/`container.conf` files for starting Components(HCD, Assembly or Container) has changed in v5.0.0.

You need to change `behaviorFactoryClassName` field to `componentHandlerClassName` & it's value will be componentHandler class.

BehaviorFactory classes (e.g, HCDBehaviorFactory) are not needed any more by the framework, you need to delete them.

For e.g, If you have HcdComponentBehaviorFactory & HcdComponentHandlers in your HCD project.
Delete `HcdComponentBehaviorFactory` file & provide class path of `HcdComponentHandlers` in `.conf` file. `csw-framework` will internally handle the subsequent steps to spawn the component.

Standalone.conf for hcd/assembly will look like this now
```hocon
prefix = "csw.samplehcd"
componentType = hcd
componentHandlerClassName = example.hcd.HcdComponentHandlers // <-- this needs to be updated.
locationServiceUsage = RegisterOnly
```

Container.conf will look like the following here onwards.
```hocon
name = "SampleContainer"
components: [
  {
    prefix = "csw.samplehcd"
    componentType = hcd
    componentHandlerClassName = "example.hcd.HcdComponentHandlers" // <-- this needs to be updated.
    locationServiceUsage = RegisterOnly
  }
]
```