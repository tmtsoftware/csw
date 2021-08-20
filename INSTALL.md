# Installing CSW

## Prerequisites

* The CSW software is build using [sbt](https://www.scala-sbt.org).
Be sure to install the latest version and add it to your shell path.

* This version of CSW also requires [Java-11](https://openjdk.java.net/projects/jdk/11/).
On Linux systems java can be installed using the standard tools (`yum` on CentOS, `dnf` on Fedora, `apt` on Ubuntu, etc.).
On Macs, you can use [AdoptOpenJDK](https://github.com/AdoptOpenJDK/homebrew-openjdk),
which is also available for [Linux](https://adoptopenjdk.net/installation.html?variant=openjdk11&jvmVariant=hotspot#x64_linux-jdk).

## Building the Software

Note: Unless you are using the latest snapshot version of CSW, it is normally not necessary to
build it yourself, since the dependencies will be automatically downloaded.

To publish CSW dependencies locally, run:

    sbt publishLocal

After publishing, you can run CSW apps using coursier as described [here](https://tmtsoftware.github.io/csw/commons/apps.html)

## Generating the documentation

The CSW [paradox](https://developer.lightbend.com/docs/paradox/current/index.html) documentation can be generated with:

    sbt clean makeSite

This puts the documentation under `target/site` with the entry point at `target/site/csw/4.0.0-RC1/index.html`.

For more details on the sbt tasks, see the [online documentation](https://tmtsoftware.github.io/csw/commons/sbt-tasks.html).

For information about making a CSW release, see [here](RELEASING.md).

**Important Note:**
Make sure you do not have spaces in the directory name or parent path where you are cloning `csw` repo.

There is a known [issue](https://github.com/lightbend/paradox/issues/387) in `paradox` plugin when you have spaces in the path, plugin fails with illegal character in path exception.
