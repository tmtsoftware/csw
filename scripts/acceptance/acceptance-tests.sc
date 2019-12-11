import $ivy.`io.get-coursier::coursier:1.1.0-M13-2`
import $ivy.`io.get-coursier::coursier-cache:1.1.0-M13-2`

import java.io.File

import coursier._
import coursier.core.Configuration
import coursier.util.Task.sync
import coursier.util._

import scala.jdk.CollectionConverters._

@main
def entryPoint(version: String, projects: String*): Unit = {
  val projectNames = if (projects.isEmpty) allProjects else projects.toList
  val results = projectNames.map { projectName =>
    projectName -> new TestModule(projectName, version).run()
  }.toMap

  println("=" * 80)
  val failedProjects = results.filterNot(_._2 == 0)
  failedProjects.keySet.foreach(project => println(s"[error] Tests failed for project: [$project]"))
  println("=" * 80)

  if (failedProjects.nonEmpty) System.exit(1)
  else System.exit(0)
}

lazy val allProjects = List(
  "csw-admin-api",
  "csw-admin-impl",
  "csw-location-agent",
  "csw-location-api",
  "csw-location-client",
  "csw-location-server",
  "csw-config-api",
  "csw-config-cli",
  "csw-config-client",
  "csw-config-server",
  "csw-logging-client",
  "csw-framework",
  "csw-params",
  "csw-command-api",
  "csw-command-client",
  "csw-event-api",
  "csw-event-cli",
  "csw-event-client",
  "csw-alarm-api",
  "csw-alarm-cli",
  "csw-alarm-client",
  "csw-database",
  "csw-aas-core",
  "csw-time-core",
  "csw-aas-http",
  "csw-aas-installed",
  "csw-time-scheduler",
  "csw-time-clock",
  "csw-network-utils"
)

class TestModule(projectName: String, version: String) {

  val testArtifact = Dependency(
    Module(
      Organization("com.github.tmtsoftware.csw"),
      ModuleName(s"${projectName}_2.12")
    ),
    version,
    Configuration.test
  )

  val cswCommons = Dependency(
    Module(
      Organization("com.github.tmtsoftware.csw"),
      ModuleName(s"csw-commons_2.12")
    ),
    version,
    Configuration.test
  )

  val files: Seq[File] = Fetch()
    .allArtifactTypes()
    .addDependencies(testArtifact, cswCommons)
    .addRepositories(Repositories.jitpack)
    .run()

  val classpath: String = files.mkString(":")

  val testJarRunpath: String =
    files
      .map(_.toString)
      .find(x => x.contains(projectName + "_") && x.contains("tests.jar"))
      .getOrElse("")

  val cmds = List(
    "java",
    "-cp",
    classpath,
    "org.scalatest.tools.Runner",
    "-oDF",
    "-C",
    "csw.commons.reporters.FileAcceptanceTestReporter",
    "-l",
    "csw.commons.tags.FileSystemSensitive",
    "-R",
    testJarRunpath
  )

  def run(): Int = {
    val builder = new ProcessBuilder(cmds.asJava).inheritIO()
    println(s"================================ Running acceptance tests for [$projectName] ================================")
    println(s"Test jar: [$testJarRunpath] ")
    println("===============================================================================================================")
    val process = builder.start()
    process.waitFor()
  }
}
