val enableCoverage         = sys.props.get("enableCoverage") == Some("true")
val MaybeCoverage: Plugins = if (enableCoverage) Coverage else Plugins.empty

val unidocExclusions: Seq[ProjectReference] = Seq(
  `csw-cluster-seed`,
  `track-location-agent`,
  `config-cli-client`,
  `integration`,
  `csw-config`
)

//Root project
lazy val `csw-prod` = project
  .in(file("."))
  .enablePlugins(UnidocSite, PublishGithub, GitBranchPrompt)
  .aggregate(`csw-location`, `track-location-agent`, `csw-cluster-seed`, `csw-config`, `config-cli-client`)
  .settings(Settings.mergeSiteWith(docs))
  .settings(Settings.docExclusions(unidocExclusions))

//Location service related projects
lazy val `csw-location` = project
  .enablePlugins(PublishBintray, GenJavadocPlugin, AutoMultiJvm, MaybeCoverage)
  .settings(
    libraryDependencies ++= Dependencies.Location
  )

lazy val `track-location-agent` = project
  .in(file("apps/track-location-agent"))
  .enablePlugins(DeployApp, MaybeCoverage)
  .dependsOn(`csw-location`)
  .settings(
    libraryDependencies ++= Dependencies.TrackLocationAgent,
    bashScriptExtraDefines ++= Seq(s"addJava -DCSW_VERSION=${version.value}")
  )

lazy val `csw-cluster-seed` = project
  .enablePlugins(DeployApp)
  .dependsOn(`csw-location`)

//Config service related projects
lazy val `config-cli-client` = project
  .in(file("apps/config-cli-client"))
  .enablePlugins(DeployApp, MaybeCoverage)
  .dependsOn(`csw-location`)
  .dependsOn(`csw-config`)
  .settings(
    libraryDependencies ++= Dependencies.ConfigCliClient,
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
  .dependsOn(`csw-location`, `track-location-agent`)
  .settings(
    libraryDependencies ++= Dependencies.Integration
  )

//Docs project
lazy val docs = project.enablePlugins(ParadoxSite, NoPublish)
