## Deploying Components

### Container for deployment

A container is a component which starts one or more Components and keeps track of the components within a single JVM process. When started, the container also registers itself with the Location Service.
The components to be hosted by the container is defined using a `ContainerInfo` model which has a set of ComponentInfo objects. It is usually described as a configuration file but can also be created programmatically.

SampleContainerInfo
:   @@@vars
    ```
    name = "Sample_Container"
    components: [
      {
        name = "SampleAssembly"
        componentType = assembly
        behaviorFactoryClassName = package.component.SampleAssembly
        prefix = abc.sample.prefix
        locationServiceUsage = RegisterAndTrackServices
        connections = [
          {
            name: Sample_Hcd_1
            componentType: hcd
            connectionType: akka
          },
          {
            name: Sample_Hcd_2
            componentType: hcd
            connectionType: akka
          },
          {
            name: Sample_Hcd_3
            componentType: hcd
            connectionType: akka
          }
        ]
      },
      {
        name = "Sample_Hcd_1"
        componentType = hcd
        behaviorFactoryClassName = package.component.SampleHcd
        prefix = abc.sample.prefix
        locationServiceUsage = RegisterOnly
      },
      {
        name = "Sample_Hcd_2"
        componentType: hcd
        behaviorFactoryClassName: package.component.SampleHcd
        prefix: abc.sample.prefix
        locationServiceUsage = RegisterOnly
      }
    ]
    ```
    @@@
    
### Standalone components

A component can be run alone in a Standalone mode without sharing it's jvm space with any other component. 