val enableCoverage         = sys.props.get("enableCoverage") == Some("true")
val MaybeCoverage: Plugins = if (enableCoverage) Coverage else Plugins.empty

lazy val aggregatedProjects: Seq[ProjectReference] = Seq(
  `csw-logging`,
  `csw-logging-macros`,
  `csw-cluster-seed`,
  `csw-config-api`,
  `csw-config-client`,
  `csw-config-client-cli`,
  `csw-config-server`,
  `csw-location`,
  `csw-location-agent`,
  `integration`
)

lazy val unidocExclusions: Seq[ProjectReference] = Seq(
  `csw-logging-macros`,
  `csw-cluster-seed`,
  `csw-location-agent`,
  `csw-config-server`,
  `csw-config-client-cli`,
  `integration`
)

//Root project
lazy val `csw-prod` = project
  .in(file("."))
  .enablePlugins(UnidocSite, PublishGithub, GitBranchPrompt)
  .aggregate(aggregatedProjects: _*)
  .settings(Settings.mergeSiteWith(docs))
  .settings(Settings.docExclusions(unidocExclusions))

lazy val `csw-logging-macros` = project
  .settings(
    libraryDependencies += Libs.`scala-reflect`
  )

//Logging service
lazy val `csw-logging` = project
  .enablePlugins(PublishBintray, GenJavadocPlugin)
  .dependsOn(`csw-logging-macros`)
  .settings(
    libraryDependencies ++= Dependencies.Logging
  )

//Location service related projects
lazy val `csw-location` = project
  .dependsOn(`csw-logging`)
  .enablePlugins(PublishBintray, GenJavadocPlugin, AutoMultiJvm, MaybeCoverage)
  .settings(
    libraryDependencies ++= Dependencies.Location
  )

//Cluster seed
lazy val `csw-cluster-seed` = project
  .enablePlugins(DeployApp)
  .dependsOn(`csw-location`)
  .settings(
    libraryDependencies ++= Dependencies.CswClusterSeed
  )

lazy val `csw-location-agent` = project
  .enablePlugins(DeployApp, MaybeCoverage)
  .dependsOn(`csw-location`)
  .settings(
    libraryDependencies ++= Dependencies.CswLocationAgent
  )

//Config service related projects
lazy val `csw-config-api` = project
  .enablePlugins(GenJavadocPlugin, MaybeCoverage)
  .dependsOn(`csw-logging`)
  .settings(
    libraryDependencies ++= Dependencies.ConfigApi
  )

lazy val `csw-config-server` = project
  .enablePlugins(DeployApp, MaybeCoverage)
  .dependsOn(`csw-location`, `csw-config-api`)
  .settings(
    libraryDependencies ++= Dependencies.ConfigServer
  )

lazy val `csw-config-client` = project
  .enablePlugins(AutoMultiJvm, MaybeCoverage)
  .dependsOn(
    `csw-config-api`,
    `csw-config-server` % "test->test",
    `csw-location`      % "compile->compile;multi-jvm->multi-jvm"
  )
  .settings(
    libraryDependencies ++= Dependencies.ConfigClient
  )

lazy val `csw-config-client-cli` = project
  .enablePlugins(AutoMultiJvm, DeployApp, MaybeCoverage)
  .dependsOn(
    `csw-config-client`,
    `csw-config-server` % "test->test",
    `csw-location`      % "multi-jvm->multi-jvm"
  )
  .settings(
    libraryDependencies ++= Dependencies.CswConfigClientCli
  )

//Integration test project
lazy val integration = project
  .enablePlugins(DeployApp)
  .dependsOn(`csw-location`, `csw-location-agent`)
  .settings(
    libraryDependencies ++= Dependencies.Integration
  )

//Docs project
lazy val docs = project.enablePlugins(ParadoxSite, NoPublish)
