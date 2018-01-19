## ComponentInfo

`ComponentInfo` model describes a component by specifying following details
It is usually described as a configuration file but can also be created programmatically.

AssemblyInfo
:   @@@vars
    ```
    name = "Sample_Assembly"
    componentType = assembly
    behaviorFactoryClassName = package.component.SampleAssembly
    prefix = abc.sample.prefix
    locationServiceUsage = RegisterAndTrackServices
    connections = [
        {
          name: "Sample_Assembly"
          componentType: assembly
          connectionType: akka
        }
      ]
    ```
    @@@
    
HcdInfo
:   @@@vars
    ```
    name = "Sample_Hcd"
    componentType = hcd
    behaviorFactoryClassName = package.component.SampleHcd
    prefix = abc.sample.prefix
    locationServiceUsage = RegisterOnly
    ```
    @@@
    
Following is the summary of properties in the ComponentInfo config/model:

* **name** : The name of the component
* **componentType** : The type of the component which could be `Container`, `Assembly`, `Hcd` or `Service`
* **behaviorFactoryClassName** : The fully qualified name of the class which extends the factory class `ComponentBehaviorFactory`
* **prefix** : A valid subsystem to which this component belongs.
* **locationServiceUsage** : Indicates how the location service should be leveraged for this component by the framework. Following values are supported:
    * DoNotRegister : Do not register this component with location service
    * RegisterOnly : Register this component with location service
    * RegisterAndTrackServices : Register this component with location service as well as track the components/services mentioned against `connections` property