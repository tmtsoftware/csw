import sbtcrossproject.CrossPlugin.autoImport.{crossProject, CrossType}
val enableCoverage         = sys.props.get("enableCoverage").contains("true")
val MaybeCoverage: Plugins = if (enableCoverage) Coverage else Plugins.empty

lazy val aggregatedProjects: Seq[ProjectReference] = Seq(
  `csw-logging`,
  `csw-logging-macros`,
  `csw-cluster-seed`,
  `csw-config-api`,
  `csw-config-client`,
  `csw-config-client-cli`,
  `csw-config-server`,
  `csw-framework`,
  `csw-command`,
  `csw-event-api`,
  `csw-event-client`,
  `csw-event-cli`,
  `csw-alarm-api`,
  `csw-alarm-client`,
  `csw-alarm-cli`,
  `csw-location`,
  `csw-location-agent`,
  `csw-benchmark`,
  `csw-messages`,
  `csw-params-jvm`,
  `csw-params-js`,
  `csw-commons`,
  `integration`,
  `examples`,
  `sequencer-prototype`,
  `romaine`
)

lazy val unidocExclusions: Seq[ProjectReference] = Seq(
  `csw-logging-macros`,
  `csw-cluster-seed`,
  `csw-location-agent`,
  `csw-config-server`,
  `csw-config-client-cli`,
  `csw-event-cli`,
  `csw-alarm-client`,
  `csw-alarm-cli`,
  `csw-commons`,
  `csw-benchmark`,
  `romaine`,
  `examples`,
  `integration`,
  `sequencer-prototype`,
  `csw-params-js`
)

lazy val githubReleases: Seq[ProjectReference] = Seq(
  `csw-cluster-seed`,
  `csw-location-agent`,
  `csw-config-server`,
  `csw-config-client-cli`,
  `csw-event-cli`
)

//Root project
lazy val `csw-prod` = project
  .in(file("."))
  .enablePlugins(NoPublish, UnidocSite, GithubPublishDocs, GitBranchPrompt, GithubRelease)
  .disablePlugins(BintrayPlugin)
  .aggregate(aggregatedProjects: _*)
  .settings(Settings.mergeSiteWith(docs))
  .settings(Settings.docExclusions(unidocExclusions))
  .settings(GithubRelease.githubReleases(githubReleases))

lazy val `csw-messages` = project
  .dependsOn(`csw-params-jvm`, `csw-commons` % "test->test")
  .enablePlugins(PublishBintray, GenJavadocPlugin)
  .settings(
    libraryDependencies ++= Dependencies.Messages.value
  )
  .settings(
    Common.detectCycles := false,
    PB.targets in Compile := Seq(
      PB.gens.java                        -> (sourceManaged in Compile).value,
      scalapb.gen(javaConversions = true) -> (sourceManaged in Compile).value
    )
  )

lazy val `csw-params` = crossProject(JSPlatform, JVMPlatform)
  .crossType(CrossType.Pure)
  .enablePlugins(PublishBintray, GenJavadocPlugin)
  .settings(
    libraryDependencies ++= Dependencies.Params.value,
    Common.detectCycles := false,
    fork := false
  )

lazy val `csw-params-js` = `csw-params`.js
lazy val `csw-params-jvm` = `csw-params`.jvm

lazy val `csw-logging-macros` = project
  .settings(
    libraryDependencies += Libs.`scala-reflect`
  )

//Logging service
lazy val `csw-logging` = project
  .dependsOn(`csw-logging-macros`, `csw-messages`)
  .enablePlugins(PublishBintray, GenJavadocPlugin, MaybeCoverage)
  .settings(
    libraryDependencies ++= Dependencies.Logging.value
  )

//Location service related projects
lazy val `csw-location` = project
  .dependsOn(
    `csw-logging`,
    `csw-messages`,
    `csw-commons` % "test->test"
  )
  .enablePlugins(PublishBintray, GenJavadocPlugin, AutoMultiJvm, MaybeCoverage)
  .settings(
    libraryDependencies ++= Dependencies.Location.value
  )

//Cluster seed
lazy val `csw-cluster-seed` = project
  .dependsOn(
    `csw-messages`,
    `csw-location`      % "compile->compile;test->test;multi-jvm->multi-jvm",
    `csw-commons`       % "compile->compile;test->test",
    `csw-framework`     % "test->test",
    `csw-config-server` % "test->test"
  )
  .enablePlugins(DeployApp, AutoMultiJvm, MaybeCoverage)
  .settings(
    libraryDependencies ++= Dependencies.ClusterSeed.value
  )

lazy val `csw-location-agent` = project
  .dependsOn(
    `csw-location`,
    `csw-commons` % "test->test"
  )
  .enablePlugins(DeployApp, MaybeCoverage)
  .settings(
    libraryDependencies ++= Dependencies.LocationAgent.value
  )

//Config service related projects
lazy val `csw-config-api` = project
  .enablePlugins(PublishBintray, GenJavadocPlugin, MaybeCoverage)
  .settings(
    libraryDependencies ++= Dependencies.ConfigApi.value
  )

lazy val `csw-config-server` = project
  .dependsOn(
    `csw-location`,
    `csw-config-api`,
    `csw-commons` % "compile->compile;test->test"
  )
  .enablePlugins(DeployApp, MaybeCoverage)
  .settings(
    libraryDependencies ++= Dependencies.ConfigServer.value
  )

lazy val `csw-config-client` = project
  .dependsOn(
    `csw-config-api`,
    `csw-commons`       % "compile->compile;test->test",
    `csw-location`      % "compile->compile;multi-jvm->multi-jvm",
    `csw-config-server` % "test->test"
  )
  .enablePlugins(PublishBintray, AutoMultiJvm, MaybeCoverage)
  .settings(
    libraryDependencies ++= Dependencies.ConfigClient.value
  )

lazy val `csw-config-client-cli` = project
  .dependsOn(
    `csw-config-client`,
    `csw-config-server` % "test->test",
    `csw-location`      % "multi-jvm->multi-jvm",
    `csw-commons`       % "test->test"
  )
  .enablePlugins(DeployApp, AutoMultiJvm, MaybeCoverage)
  .settings(
    libraryDependencies ++= Dependencies.ConfigClientCli.value
  )

lazy val `csw-command` = project
  .dependsOn(`csw-messages`, `csw-logging`)
  .enablePlugins(PublishBintray, AutoMultiJvm, GenJavadocPlugin, MaybeCoverage)
  .settings(libraryDependencies ++= Dependencies.Command.value)

lazy val `csw-framework` = project
  .dependsOn(
    `csw-messages`,
    `csw-config-client`,
    `csw-logging`,
    `csw-command`,
    `csw-event-api`,
    `csw-event-client`,
    `csw-alarm-client`,
    `csw-event-client`  % "test->test",
    `csw-location`      % "compile->compile;multi-jvm->multi-jvm",
    `csw-config-server` % "multi-jvm->test",
    `csw-commons`       % "test->test"
  )
  .enablePlugins(PublishBintray, AutoMultiJvm, GenJavadocPlugin, CswBuildInfo, MaybeCoverage)
  .settings(
    libraryDependencies ++= Dependencies.Framework.value
  )

lazy val `csw-event-api` = project
  .dependsOn(`csw-messages`)
  .enablePlugins(PublishBintray, GenJavadocPlugin)

lazy val `csw-event-client` = project
  .dependsOn(
    `csw-event-api`,
    `csw-logging`,
    `romaine`,
    `csw-location` % "compile->compile;multi-jvm->multi-jvm",
    `csw-commons`  % "test->test"
  )
  .enablePlugins(PublishBintray, AutoMultiJvm, GenJavadocPlugin, MaybeCoverage)
  .settings(libraryDependencies ++= Dependencies.EventClient.value)

lazy val `csw-event-cli` = project
  .dependsOn(
    `csw-messages`,
    `csw-event-client`,
    `csw-cluster-seed` % "test->multi-jvm",
    `csw-commons`      % "test->test"
  )
  .enablePlugins(DeployApp, MaybeCoverage)
  .settings(libraryDependencies ++= Dependencies.EventCli.value)

lazy val `csw-alarm-api` = project
  .dependsOn(`csw-messages`)
  .enablePlugins(PublishBintray, GenJavadocPlugin)
  .settings(libraryDependencies ++= Dependencies.AlarmApi.value)

lazy val `csw-alarm-client` = project
  .dependsOn(
    `csw-alarm-api`,
    `csw-location`,
    `csw-logging`,
    `romaine`,
    `csw-commons` % "test->test"
  )
  .enablePlugins(PublishBintray, GenJavadocPlugin, MaybeCoverage)
  .settings(libraryDependencies ++= Dependencies.AlarmClient.value)

lazy val `csw-alarm-cli` = project
  .dependsOn(
    `csw-alarm-client`,
    `csw-config-client`,
    `csw-cluster-seed` % "test->multi-jvm"
  )
  .enablePlugins(DeployApp, MaybeCoverage)
  .settings(libraryDependencies ++= Dependencies.AlarmCli.value)

lazy val `csw-commons` = project
  .enablePlugins(PublishBintray, GenJavadocPlugin)
  .settings(
    libraryDependencies ++= Dependencies.Commons.value
  )

lazy val `romaine` = project
  .enablePlugins(PublishBintray)
  .settings(
    libraryDependencies ++= Dependencies.Romaine.value
  )

lazy val `csw-benchmark` = project
  .dependsOn(
    `csw-logging`,
    `csw-messages`,
    `csw-framework` % "compile->compile;test->test",
    `csw-command`
  )
  .enablePlugins(NoPublish, JmhPlugin)
  .disablePlugins(BintrayPlugin)
  .settings(
    libraryDependencies ++= Dependencies.Benchmark.value
  )

//Integration test project
lazy val integration = project
  .dependsOn(`csw-location`, `csw-location-agent`)
  .enablePlugins(DeployApp)
  .settings(
    libraryDependencies ++= Dependencies.Integration.value
  )

//Docs project
lazy val docs = project.enablePlugins(NoPublish, ParadoxSite)

//Example code
lazy val examples = project
  .dependsOn(
    `csw-location`,
    `csw-config-client`,
    `csw-config-server` % "test->test",
    `csw-logging`,
    `csw-messages`,
    `csw-framework`
  )
  .enablePlugins(DeployApp)
  .settings(
    libraryDependencies ++= Dependencies.Examples.value
  )

lazy val `sequencer-prototype` = project
  .dependsOn(
    `csw-location`,
    `csw-config-client`,
    `csw-framework`,
    `csw-command`
  )
  .enablePlugins(NoPublish)
  .disablePlugins(BintrayPlugin)
  .settings(
    libraryDependencies ++= Dependencies.SequencerPrototype.value
  )

lazy val alarm = taskKey[Unit]("alarm")

alarm := {
  (test in (`csw-alarm-client`, Test)).value
  (test in (`csw-alarm-api`, Test)).value
  (test in (`csw-alarm-cli`, Test)).value
}
