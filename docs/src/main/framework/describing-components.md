# ComponentInfo

The `ComponentInfo` model describes a component by specifying several details.
It is usually described as a configuration file called the "Component Info File" but can also be created programmatically.

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
(or `JComponentBehaviorFactory`, if written in Java)
* **prefix** : A valid subsystem to which this component belongs.
* **connections** : A collection of `connections` of the components or services which will be used by this component. This information can 
be used in accordance with the `locationServiceUsage` property to automatically track these components or services by the framework.
* **locationServiceUsage** : Indicates how the Location Service should be leveraged for this component by the framework. The following values are supported:
    * DoNotRegister : Do not register this component with the Location Service
    * RegisterOnly : Register this component with the Location Service
    * RegisterAndTrackServices : Register this component with the Location Service as well as track the components/services listed in the `connections` property
* **initializeTimeout** (Optional) : An optional parameter that specifies the timeout time for the initialization phase of the component lifecycle (see  
@ref:[Component Handlers](handling-lifecycle.md)).  This is specified using a whole number and then units (e.g. "5 seconds"), where the units