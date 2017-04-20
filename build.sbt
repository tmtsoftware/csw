val enableCoverage         = sys.props.get("enableCoverage") == Some("true")
val MaybeCoverage: Plugins = if (enableCoverage) Coverage else Plugins.empty

val unidocExclusions: Seq[ProjectReference] = Seq(
  `csw-cluster-seed`,
  `csw-location-agent`,
  `csw-config-client-cli`,
  `integration`,
  `csw-config`
)

//Root project
lazy val `csw-prod` = project
  .in(file("."))
  .enablePlugins(UnidocSite, PublishGithub, GitBranchPrompt)
  .aggregate(`csw-location`, `csw-location-agent`, `csw-cluster-seed`, `csw-config`, `csw-config-client-cli`)
  .settings(Settings.mergeSiteWith(docs))
  .settings(Settings.docExclusions(unidocExclusions))

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

lazy val `csw-cluster-seed` = project
  .enablePlugins(DeployApp)
  .dependsOn(`csw-location`)

//Config service related projects
lazy val `csw-config-client-cli` = project
  .enablePlugins(DeployApp, MaybeCoverage)
  .dependsOn(`csw-location`)
  .dependsOn(`csw-config`)
  .settings(
    libraryDependencies ++= Dependencies.CswConfigClientCli,
    bashScriptExtraDefines ++= Seq(s"addJava -DCSW_VERSION=${version.value}")
  )

lazy val `csw-config-api` = project
  .enablePlugins(DeployApp, MaybeCoverage)
  .settings(
    libraryDependencies ++= Dependencies.ConfigApi,
    bashScriptExtraDefines ++= Seq(s"addJava -DCSW_VERSION=${version.value}")
  )

lazy val `csw-config` = project
  .enablePlugins(DeployApp, AutoMultiJvm, MaybeCoverage)
  .dependsOn(`csw-location`)
  .settings(
    libraryDependencies ++= Dependencies.Config,
    bashScriptExtraDefines ++= Seq(s"addJava -DCSW_VERSION=${version.value}")
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
