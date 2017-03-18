import Dependencies._


val enableCoverage = System.getProperty("enableCoverage", "true")
val plugins:Seq[Plugins] = if(enableCoverage.toBoolean) Seq(Coverage) else Seq.empty

lazy val csw = project
  .in(file("."))
  .enablePlugins(UnidocSite, PublishGithub, GitBranchPrompt)
  .aggregate(`csw-location`, trackLocation, docs, integration)
  .settings(Settings.mergeSiteWith(docs))
  .settings(
    unidocProjectFilter in (ScalaUnidoc, unidoc) := inAnyProject -- inProjects(trackLocation, integration),
    aggregate in test := false
  )


lazy val `csw-location` = project
  .enablePlugins(PublishBintray, GenJavadocPlugin)
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
      `scalamock-scalatest-support` % Test,
      `junit` % Test,
      `junit-interface` % Test
    )
  )

lazy val trackLocation = project
  .in(file("apps/trackLocation"))
  .enablePlugins(DeployApp)
  .dependsOn(`csw-location`)
  .settings(
    libraryDependencies ++= Seq(
      `akka-actor`,
      `scopt`,
      `scalatest` % Test,
      `scalamock-scalatest-support` % Test,
      `scala-logging` % Test
    ),
    sources in (Compile, doc) := Seq.empty
  )

lazy val docs = project
  .enablePlugins(ParadoxSite, NoPublish)

lazy val integration = project
  .enablePlugins(DeployApp)
  .dependsOn(`csw-location`)
  .dependsOn(trackLocation)
  .settings(
    libraryDependencies ++= Seq(
      `akka-stream-testkit`,
      `scalatest`,
      `scalamock-scalatest-support`,
      `jmdns`
    ),
    sources in Test := (sources in Compile).value
  )
