val enableCoverage =  sys.props.get("enableCoverage") == Some("true")
val MaybeCoverage: Plugins = if(enableCoverage) Coverage else Plugins.empty

lazy val `csw-prod` = project
  .in(file("."))
  .enablePlugins(UnidocSite, PublishGithub, GitBranchPrompt)
  .aggregate(`csw-location`, `track-location-agent`, `csw-cluster-seed`, `csw-config`, `config-cli-client`)
  .settings(Settings.mergeSiteWith(docs))
  .settings(
    unidocProjectFilter in (ScalaUnidoc, unidoc) := inAnyProject -- inProjects(
      `csw-cluster-seed`,
      `track-location-agent`,
      `config-cli-client`,
      `integration`,
      `csw-config`
    )
  )

lazy val `csw-location` = project
  .enablePlugins(PublishBintray, GenJavadocPlugin, AutoMultiJvm, MaybeCoverage)
  .settings(
    libraryDependencies ++= Dependencies.Location
  )

lazy val `track-location-agent` = project
  .in(file("apps/track-location-agent"))
  .enablePlugins(DeployApp)
  .dependsOn(`csw-location`)
  .settings(
    libraryDependencies ++= Dependencies.TrackLocationAgent,
    sources in (Compile, doc) := Seq.empty,
    bashScriptExtraDefines ++= Seq(s"addJava -DCSW_VERSION=${version.value}")
  )

lazy val `config-cli-client` = project
  .in(file("apps/config-cli-client"))
  .enablePlugins(DeployApp)
  .dependsOn(`csw-location`)
  .dependsOn(`csw-config`)
  .settings(
    libraryDependencies ++= Dependencies.ConfigCliClient,
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

lazy val docs = project.enablePlugins(ParadoxSite, NoPublish)

lazy val integration = project
  .enablePlugins(DeployApp)
  .dependsOn(`csw-location`, `track-location-agent`)
  .settings(
    libraryDependencies ++= Dependencies.Integration,
    sources in Test := (sources in Compile).value
  )


lazy val `csw-config` = project
  .enablePlugins(DeployApp, AutoMultiJvm)
  .dependsOn(`csw-location`)
  .settings(
    libraryDependencies ++= Dependencies.Config,
    sources in (Compile, doc) := Seq.empty,
    bashScriptExtraDefines ++= Seq(s"addJava -DCSW_VERSION=${version.value}")
  )
