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
  `csw-params`,
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
  `sequencer-prototype`
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
  .dependsOn(`csw-params`, `csw-commons` % "test->test")
  .enablePlugins(PublishBintray, GenJavadocPlugin)
  .settings(
    libraryDependencies ++= Dependencies.Messages
  )
  .settings(
    Common.detectCycles := false,
    PB.targets in Compile := Seq(
      PB.gens.java                        -> (sourceManaged in Compile).value,
      scalapb.gen(javaConversions = true) -> (sourceManaged in Compile).value
    )
  )

lazy val `csw-params` = project
  .dependsOn(`csw-commons` % "test->test")
  .enablePlugins(PublishBintray, GenJavadocPlugin)
  .settings(
    libraryDependencies ++= Dependencies.Params,
    Common.detectCycles := false
  )

lazy val `csw-logging-macros` = project
  .settings(
    libraryDependencies += Libs.`scala-reflect`
  )

//Logging service
lazy val `csw-logging` = project
  .dependsOn(`csw-logging-macros`, `csw-messages`)
  .enablePlugins(PublishBintray, GenJavadocPlugin, MaybeCoverage)
  .settings(
    libraryDependencies ++= Dependencies.Logging
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
    libraryDependencies ++= Dependencies.Location
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
    libraryDependencies ++= Dependencies.ClusterSeed
  )

lazy val `csw-location-agent` = project
  .dependsOn(
    `csw-location`,
    `csw-commons` % "test->test"
  )
  .enablePlugins(DeployApp, MaybeCoverage)
  .settings(
    libraryDependencies ++= Dependencies.LocationAgent
  )

//Config service related projects
lazy val `csw-config-api` = project
  .enablePlugins(PublishBintray, GenJavadocPlugin, MaybeCoverage)
  .settings(
    libraryDependencies ++= Dependencies.ConfigApi
  )

lazy val `csw-config-server` = project
  .dependsOn(
    `csw-location`,
    `csw-config-api`,
    `csw-commons` % "compile->compile;test->test"
  )
  .enablePlugins(DeployApp, MaybeCoverage)
  .settings(
    libraryDependencies ++= Dependencies.ConfigServer
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
    libraryDependencies ++= Dependencies.ConfigClient
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
    libraryDependencies ++= Dependencies.ConfigClientCli
  )

lazy val `csw-command` = project
  .dependsOn(`csw-messages`, `csw-logging`)
  .enablePlugins(PublishBintray, AutoMultiJvm, GenJavadocPlugin, MaybeCoverage)
  .settings(libraryDependencies ++= Dependencies.Command)

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
    libraryDependencies ++= Dependencies.Framework
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
  .settings(libraryDependencies ++= Dependencies.EventClient)

lazy val `csw-event-cli` = project
  .dependsOn(
    `csw-messages`,
    `csw-event-client`,
    `csw-cluster-seed` % "test->multi-jvm",
    `csw-commons`      % "test->test"
  )
  .enablePlugins(DeployApp, MaybeCoverage)
  .settings(libraryDependencies ++= Dependencies.EventCli)

lazy val `csw-alarm-api` = project
  .dependsOn(`csw-messages`)
  .enablePlugins(PublishBintray, GenJavadocPlugin)
  .settings(libraryDependencies ++= Dependencies.AlarmApi)

lazy val `csw-alarm-client` = project
  .dependsOn(
    `csw-alarm-api`,
    `csw-location`,
    `csw-logging`,
    `romaine`,
    `csw-commons` % "test->test"
  )
  .enablePlugins(PublishBintray, GenJavadocPlugin, MaybeCoverage)
  .settings(libraryDependencies ++= Dependencies.AlarmClient)

lazy val `csw-alarm-cli` = project
  .dependsOn(
    `csw-alarm-client`,
    `csw-config-client`,
    `csw-cluster-seed` % "test->multi-jvm"
  )
  .enablePlugins(DeployApp, MaybeCoverage)
  .settings(libraryDependencies ++= Dependencies.AlarmCli)

lazy val `csw-commons` = project
  .enablePlugins(PublishBintray, GenJavadocPlugin)
  .settings(
    libraryDependencies ++= Dependencies.Commons
  )

lazy val `romaine` = project
  .enablePlugins(PublishBintray)
  .settings(
    libraryDependencies ++= Dependencies.Romaine
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
    libraryDependencies ++= Dependencies.Benchmark
  )

//Integration test project
lazy val integration = project
  .dependsOn(`csw-location`, `csw-location-agent`)
  .enablePlugins(DeployApp)
  .settings(
    libraryDependencies ++= Dependencies.Integration
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
    libraryDependencies ++= Dependencies.Examples
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
    libraryDependencies ++= Dependencies.SequencerPrototype
  )

lazy val alarm = taskKey[Unit]("alarm")

alarm := {
  (test in (`csw-alarm-client`, Test)).value
  (test in (`csw-alarm-api`, Test)).value
  (test in (`csw-alarm-cli`, Test)).value
}