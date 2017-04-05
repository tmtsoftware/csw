val enableCoverage = System.getProperty("enableCoverage", "true")
val plugins:Seq[Plugins] = if(enableCoverage.toBoolean) Seq(Coverage) else Seq.empty

lazy val `csw-prod` = project
  .in(file("."))
  .enablePlugins(UnidocSite, PublishGithub, GitBranchPrompt)
  .aggregate(`csw-location`, `track-location-agent`, `csw-cluster-seed`, `csw-cs`)
  .settings(Settings.mergeSiteWith(docs))
  .settings(
    unidocProjectFilter in (ScalaUnidoc, unidoc) := inAnyProject -- inProjects(
      `csw-cluster-seed`,
      `track-location-agent`,
      `integration`,
      `csw-cs`
    )
  )

lazy val `csw-location` = project
  .enablePlugins(PublishBintray, GenJavadocPlugin, AutoMultiJvm)
  .enablePlugins(plugins:_*)
  .settings(
    libraryDependencies ++= Seq(
      Akka.`akka-stream`,
      Akka.`akka-distributed-data`,
      Akka.`akka-remote`,
      Akka.`akka-cluster-tools`,
      Libs.`scala-java8-compat`,
      Libs.`scala-async`,
      Libs.`enumeratum`,
      Libs.`chill-akka`,
      Libs.`akka-management-cluster-http`,
      AkkaHttp.`akka-http`
    ),
    libraryDependencies ++= Seq(
      Akka.`akka-stream-testkit` % Test,
      Libs.`scalatest` % Test,
      Libs.`junit` % Test,
      Libs.`junit-interface` % Test,
      Libs.`mockito-core` % Test,
      Akka.`akka-multi-node-testkit` % Test
    )
  )

lazy val `track-location-agent` = project
  .in(file("apps/track-location-agent"))
  .enablePlugins(DeployApp)
  .dependsOn(`csw-location`)
  .settings(
    libraryDependencies ++= Seq(
      Akka.`akka-actor`,
      Libs.`scopt`,
      Libs.`scalatest` % Test,
      Libs.`scala-logging` % Test
    ),
    sources in (Compile, doc) := Seq.empty,
    bashScriptExtraDefines ++= Seq(s"addJava -DCSW_VERSION=${version.value}")
  )

lazy val `csw-cluster-seed` = project
  .in(file("apps/csw-cluster-seed"))
  .enablePlugins(DeployApp)
  .dependsOn(`csw-location`)
  .settings(
    sources in (Compile, doc) := Seq.empty
  )

lazy val docs = project
  .enablePlugins(ParadoxSite, NoPublish)

lazy val integration = project
  .enablePlugins(DeployApp)
  .dependsOn(`csw-location`)
  .dependsOn(`track-location-agent`)
  .settings(
    libraryDependencies ++= Seq(
      Libs.`scalatest`,
      Akka.`akka-stream-testkit`
    ),
    sources in Test := (sources in Compile).value
  )


lazy val `csw-cs` = project
  .enablePlugins(DeployApp)
  .dependsOn(`csw-location`)
  .settings(
    libraryDependencies ++= Seq(
      AkkaHttp.`akka-http`,
      Libs.svnkit,
      Libs.`play-json`,
      Libs.`scopt`
    ),
    libraryDependencies ++= Seq(
      Libs.`scalatest` % Test
    ),
    sources in (Compile, doc) := Seq.empty,
    bashScriptExtraDefines ++= Seq(s"addJava -DCSW_VERSION=${version.value}")
  )
