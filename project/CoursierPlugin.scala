import sbt.Keys._
import sbt.{AutoPlugin, Def, Task, _}

import scala.language.postfixOps
import scala.sys.process._

object CoursierPlugin extends AutoPlugin {
  val aggregateFilter = ScopeFilter(inAggregates(ThisProject), inConfigurations(Compile))

  object autoImport {
    val Coursier: Configuration = config("coursier")
    val bootstrap = TaskKey[Unit](
      "bootstrap",
      "Create a local directory with all the executable files."
    )
  }

  import autoImport._

  override def projectConfigurations: Seq[Configuration] = Seq(Coursier)

  def bootstrapTask(projects: Seq[ProjectReference]): Def.Initialize[Task[Int]] = Def.task {
    projects.map(publishLocal in _).join.value
    val dir = baseDirectory.value / "scripts"
    s"sh $dir/csw-bootstrap.sh ${version.value} ${baseDirectory.value}" !
  }
}
