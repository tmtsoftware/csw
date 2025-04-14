# Migration Guide from 5.0.0 to 6.0.0

This guide focuses on changes that may require current code to be changed to go from 
CSW Version 5.0.0 to CSW Version 6.0.0. From the release notes, the following are
the important changes from 5.0.0 that may require code changes:

In this version of CSW, the [Akka](https://akka.io/) library has been replaced with [Apache Pekko](https://pekko.apache.org/),
due to [Akka licensing changes](https://akka.io/blog/why-we-are-changing-the-license-for-akka).
This is mostly just a matter of replacing "akka" with "pekko" in the source code and

    import akka.*

with 

    import org.apache.pekko.*

The library names changed from, for example, `akka-actor` to `pekko-actor`.
The CSW class `AkkaConnection` was renamed to `PekkoConnection` and in config files, the connection type `akka` was replaced with `pekko`.
In reference.conf and application.conf files, replace `akka` with `pekko`.

See the [Pekko Migration Guide](https://pekko.apache.org/docs/pekko/1.0/project/migration-guides.html) for more information.

This version of CSW has also moved to scala-3 (scala-3.6.4) and java-21. Please make sure that java-21 is set to be the default
when running or building csw.
If you are using async/await in Scala sources, you should replace:

    import scala.async.Async._

with 

    import cps.compat.FutureAsync.*

and replace the `scala-async` library dependency with `dotty-cps-async`.
For more information, see the [Scala 3 Migration Guide](https://docs.scala-lang.org/scala3/guides/migration/compatibility-intro.html).

Note that in scala-3, main application objects no longer extend `App`. Instead, you can use declare aan object with a main() function or
use a number of other methods, as described in [Main Methods in Scala 3](https://docs.scala-lang.org/scala3/book/methods-main-methods.html).

For Scala classes that call Java code, replace:

    import scala.compat.java8.FutureConverters.CompletionStageOps

with:

    import scala.jdk.FutureConverters.*
