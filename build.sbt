import Dependencies._

lazy val csw = project
  .in(file("."))
  .enablePlugins(UnidocSite, PublishGithub, PublishBintray)
  .aggregate(`csw-location`, `integration-tests`)
  .settings(Settings.mergeSiteWith(docs))

lazy val `csw-location` = project
  .enablePlugins(Coverage, PublishBintray)
  .settings(
    libraryDependencies ++= Seq(
      `akka-stream`,
      `jmdns`,
      `scala-java8-compat`,
      `akka-remote`,
      `scala-async`
    ),
    libraryDependencies ++= Seq(
      `akka-stream-testkit` % Test,
      `scalatest` % Test,
      `scalamock-scalatest-support` % Test
    )
  )

lazy val `trackLocation` = Project(id = "trackLocation", base = file("apps/trackLocation"))
  .enablePlugins(Coverage, PublishBintray)
  .settings(
    libraryDependencies ++= Seq(
      `akka-actor`,
      `scopt`,
      `scalatest` % Test,
      `scalamock-scalatest-support` % Test
    )
  ) dependsOn(`csw-location`)
lazy val `integration-tests` = project
  .dependsOn(`csw-location` % "compile->compile;test->test")

lazy val docs = project
  .enablePlugins(ParadoxSite, NoPublish)

mainClass in (Compile,run) := Some("csw.services.location.integration.HCDApp")