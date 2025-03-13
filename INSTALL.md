# Installing CSW

## Prerequisites

* The CSW software is build using [sbt](https://www.scala-sbt.org).
Be sure to install the latest version and add it to your shell path.

* This version of CSW also requires [Java-21](https://openjdk.java.net/projects/jdk/21/).

## Upgrading to Java 21

If you have Java installed via Coursier then follow these instructions:
- Re-install `cs` as described in [Coursier installation](https://get-coursier.io/docs/cli-installation#native-launcher) (this will make sure that jvm-indexes are updated).
- Run the following command `cs java --jvm temurin:1.21` - this will upgrade Java to version 21.
- Once Java is upgraded include command `eval $(cs java --jvm temurin:1.21 --env)` in your respective shell profiles (this will set the required environment variables (e.g., JAVA_HOME) into your shell).

If you want to install Java 21 manually - the binary package for macOS or Linux can be downloaded from [Adoptium Temurin releases](https://adoptium.net/temurin/releases).

## Building the Software

Note: Unless you are using the latest snapshot version of CSW, it is normally not necessary to
build it yourself, since the dependencies will be automatically downloaded.

To publish CSW dependencies locally, run:

    sbt publishLocal

After publishing, you can run CSW apps using coursier as described [here](https://tmtsoftware.github.io/csw/commons/apps.html)

## Generating the documentation

The CSW [paradox](https://developer.lightbend.com/docs/paradox/current/index.html) documentation can be generated with:

    sbt clean makeSite

This puts the documentation under `target/site` with the entry point at `target/site/csw/4.0.1/index.html`.

For more details on the sbt tasks, see the [online documentation](https://tmtsoftware.github.io/csw/commons/sbt-tasks.html).

For information about making a CSW release, see [here](RELEASING.md).

**Important Note:**
Make sure you do not have spaces in the directory name or parent path where you are cloning `csw` repo.

There is a known [issue](https://github.com/lightbend/paradox/issues/387) in `paradox` plugin when you have spaces in the path, plugin fails with illegal character in path exception.
