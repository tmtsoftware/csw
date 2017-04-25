# Configuration service

Configuration Service provides a centralized persistent store for any configuration file used in the TMT Software System. All versions of configuration files are retained providing a historical record of each configuration file.
 

## Artifacts

sbt
:   @@@vars
    ```scala
    libraryDependencies += "org.tmt" %% "csw-config_$scala.binaryVersion$" % "$version$"
    ```
    @@@

maven
:   @@@vars
    ```xml
    <dependency>
     <groupId>org.tmt</groupId>
     <artifactId>csw-config_$scala.binaryVersion$</artifactId>
     <version>$version$</version>
     <type>pom</type>
    </dependency>
    ```
    @@@

gradle
:   @@@vars
    ```gradle
    dependencies {
      compile group: "org.tmt", name: "csw-config_$scala.binaryVersion$", version: "$version$"
    }
    ```
    @@@