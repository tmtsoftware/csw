import Dependencies._


val enableCoverage = System.getProperty("enableCoverage", "true")
val plugins:Seq[Plugins] = if(enableCoverage.toBoolean)
  Seq(Coverage, PublishBintray)
else
  Seq(PublishBintray)

lazy val csw = project
  .in(file("."))
  .enablePlugins(UnidocSite, PublishGithub, PublishBintray, GitBranchPrompt)
  .aggregate(`csw-location`)
  .settings(Settings.mergeSiteWith(docs))



lazy val `csw-location` = project
  .enablePlugins(plugins:_*)
  .settings(
    libraryDependencies ++= Seq(
      `akka-stream`,
      `jmdns`,
      `scala-java8-compat`,
      `akka-remote`,
      `scala-async`,
      `enumeratum`
    ),
    libraryDependencies ++= Seq(
      `akka-stream-testkit` % Test,
      `scalatest` % Test,
      `scalamock-scalatest-support` % Test
    )
  )

lazy val `trackLocation` = project
  .in(file("apps/trackLocation"))
  .enablePlugins(Coverage, PublishBintray)
  .settings(
    libraryDependencies ++= Seq(
      `akka-actor`,
      `scopt`,
      `scalatest` % Test,
      `scalamock-scalatest-support` % Test,
      `scala-logging` % Test
    )
  )
  .dependsOn(`csw-location`)

lazy val docs = project
  .enablePlugins(ParadoxSite, NoPublish)