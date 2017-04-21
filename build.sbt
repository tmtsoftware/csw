val enableCoverage         = sys.props.get("enableCoverage") == Some("true")
val MaybeCoverage: Plugins = if (enableCoverage) Coverage else Plugins.empty

lazy val aggregatedProjects: Seq[ProjectReference] = Seq(
  `csw-cluster-seed`,
  `csw-config-api`,
  `csw-config-client`,
  `csw-config-client-cli`,
  `csw-config-server`,
  `csw-location`,
  `csw-location-agent`,
  `csw-test-utils`
)

lazy val unidocExclusions: Seq[ProjectReference] = Seq(
  `csw-cluster-seed`,
  `csw-location-agent`,
  `csw-config-api`,
  `csw-config-server`,
  `csw-config-client-cli`,
  `integration`,
  `csw-test-utils`
)

//Root project
lazy val `csw-prod` = project
  .in(file("."))
  .enablePlugins(UnidocSite, PublishGithub, GitBranchPrompt)
  .aggregate(aggregatedProjects: _*)
  .settings(Settings.mergeSiteWith(docs))
  .settings(Settings.docExclusions(unidocExclusions))

//Cluster seed
lazy val `csw-cluster-seed` = project
  .enablePlugins(DeployApp)
  .dependsOn(`csw-location`)
  .settings(
    libraryDependencies ++= Dependencies.CswClusterSeed
  )

//Location service related projects
lazy val `csw-location` = project
  .enablePlugins(PublishBintray, GenJavadocPlugin, AutoMultiJvm, MaybeCoverage)
  .settings(
    libraryDependencies ++= Dependencies.Location
  )

lazy val `csw-location-agent` = project
  .enablePlugins(DeployApp, MaybeCoverage)
  .dependsOn(`csw-location`)
  .settings(
    libraryDependencies ++= Dependencies.CswLocationAgent,
    bashScriptExtraDefines ++= Seq(s"addJava -DCSW_VERSION=${version.value}")
  )

//Config service related projects
lazy val `csw-config-api` = project
  .enablePlugins(MaybeCoverage)
  .dependsOn(`csw-test-utils` % Test)
  .settings(
    libraryDependencies ++= Dependencies.ConfigApi
  )

lazy val `csw-config-server` = project
  .enablePlugins(DeployApp, MaybeCoverage)
  .dependsOn(`csw-location`, `csw-config-api`)
  .settings(
    libraryDependencies ++= Dependencies.ConfigServer,
    bashScriptExtraDefines ++= Seq(s"addJava -DCSW_VERSION=${version.value}")
  )

lazy val `csw-config-client` = project
  .enablePlugins(AutoMultiJvm, MaybeCoverage)
  .dependsOn(`csw-location`, `csw-config-api`, `csw-config-server` % "test->test")
  .settings(
    libraryDependencies ++= Dependencies.ConfigClient
  )

lazy val `csw-config-client-cli` = project
  .enablePlugins(DeployApp, MaybeCoverage)
  .dependsOn(`csw-config-client`, `csw-config-server` % "test->test")
  .settings(
    libraryDependencies ++= Dependencies.CswConfigClientCli,
    bashScriptExtraDefines ++= Seq(s"addJava -DCSW_VERSION=${version.value}")
  )

//csw test utils
lazy val `csw-test-utils` = project
  .enablePlugins(AutoMultiJvm)
  .settings(
    libraryDependencies ++= Dependencies.TestUtils
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
