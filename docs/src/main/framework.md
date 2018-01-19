# Framework for creating components (HCD, Assembly, Container)

**csw-framework** library provides support for creating a component as defined by the TMT. 

## Artifacts

sbt
:   @@@vars
    ```scala
    libraryDependencies += "org.tmt" %% "csw-framework" % "$version$"
    ```
    @@@

maven
:   @@@vars
    ```xml
    <dependency>
     <groupId>org.tmt</groupId>
     <artifactId>csw-framework_$scala.binaryVersion$</artifactId>
     <version>$version$</version>
     <type>pom</type>
    </dependency>
    ```
    @@@

gradle
:   @@@vars
    ```gradle
    dependencies {
      compile group: "org.tmt", name: "csw-framework_$scala.binaryVersion$", version: "$version$"
    }
    ```
    @@@
    
@@toc { depth=1 } 
    
@@@index
* [Describing Components](framework/describing-components.md)
* [Creating Components](framework/creating-components.md)
* [Handling Lifecycle](framework/handling-lifecycle.md)
* [Tracking Connections](framework/tracking-connections.md)
* [Publishing State](framework/publishing-state.md)
* [Handling Exceptions](framework/handling-exceptions.md)
* [Deployment](framework/deploying-components.md)
@@@
    



