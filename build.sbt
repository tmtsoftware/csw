val enableCoverage = sys.props.get("enableCoverage").contains("true")
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
  `csw-location`,
  `csw-location-agent`,
  `csw-benchmark`,
  `csw-vslice`,
  `csw-messages`,
  `csw-commons`,
  `integration`,
  `examples`
)

lazy val unidocExclusions: Seq[ProjectReference] = Seq(
  `csw-logging-macros`,
  `csw-cluster-seed`,
  `csw-location-agent`,
  `csw-config-server`,
  `csw-config-client-cli`,
  `csw-benchmark`,
  `csw-vslice`,
  `integration`,
  `examples`
)

lazy val githubReleases: Seq[ProjectReference] = Seq(
  `csw-cluster-seed`,
  `csw-location-agent`,
  `csw-config-server`
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
  .enablePlugins(PublishBintray, GenJavadocPlugin)
  .settings(
    libraryDependencies ++= Dependencies.Messages
  )
  .settings(
    Common.detectCycles := false,
    PB.targets in Compile := Seq(
      PB.gens.java -> (sourceManaged in Compile).value,
      scalapb.gen(javaConversions = true) -> (sourceManaged in Compile).value
    )
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
  .dependsOn(`csw-logging`, `csw-messages`)
  .enablePlugins(PublishBintray, GenJavadocPlugin, AutoMultiJvm, MaybeCoverage)
  .settings(
    libraryDependencies ++= Dependencies.Location
  )

//Cluster seed
lazy val `csw-cluster-seed` = project
  .dependsOn(
    `csw-messages`,
    `csw-location`,
    `csw-commons`,
    `csw-framework` % "test->test",
    `csw-config-server` % "test->test",
  )
  .enablePlugins(DeployApp, MaybeCoverage)
  .settings(
    libraryDependencies ++= Dependencies.ClusterSeed
  )

lazy val `csw-location-agent` = project
  .dependsOn(`csw-location`)
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
  .dependsOn(`csw-location`, `csw-config-api`, `csw-commons`)
  .enablePlugins(DeployApp, MaybeCoverage)
  .settings(
    libraryDependencies ++= Dependencies.ConfigServer
  )

lazy val `csw-config-client` = project
  .dependsOn(
    `csw-config-api`,
    `csw-commons`,
    `csw-config-server` % "test->test",
    `csw-location` % "compile->compile;multi-jvm->multi-jvm"
  )
  .enablePlugins(PublishBintray, AutoMultiJvm, MaybeCoverage)
  .settings(
    libraryDependencies ++= Dependencies.ConfigClient
  )

lazy val `csw-config-client-cli` = project
  .dependsOn(
    `csw-config-client`,
    `csw-config-server` % "test->test",
    `csw-location` % "multi-jvm->multi-jvm"
  )
  .enablePlugins(DeployApp, AutoMultiJvm, MaybeCoverage)
  .settings(
    libraryDependencies ++= Dependencies.ConfigClientCli
  )

lazy val `csw-vslice` = project
  .dependsOn(
    `csw-framework`,
    `csw-command`
  )
  .enablePlugins(DeployApp)

lazy val `csw-event-api` = project
  .dependsOn(
    `csw-messages`
  )
  .enablePlugins(GenJavadocPlugin)

lazy val `csw-event-impl` = project
  .dependsOn(
    `csw-event-api`,
    `csw-logging`
  )
  .enablePlugins(AutoMultiJvm)
  .settings(libraryDependencies ++= Dependencies.EventImpl)

lazy val `csw-command` = project
  .dependsOn(
    `csw-messages`,
    `csw-logging`
  )
  .enablePlugins(PublishBintray, AutoMultiJvm, GenJavadocPlugin)
  .settings(libraryDependencies ++= Dependencies.Command)

lazy val `csw-framework` = project
  .dependsOn(
    `csw-messages`,
    `csw-config-client`,
    `csw-logging`,
    `csw-command`,
    `csw-location` % "compile->compile;multi-jvm->multi-jvm",
    `csw-config-server` % "multi-jvm->test"
  )
  .enablePlugins(PublishBintray, AutoMultiJvm, GenJavadocPlugin, CswBuildInfo)
  .settings(
    libraryDependencies ++= Dependencies.Framework
  )


lazy val `csw-commons` = project
  .enablePlugins(PublishBintray, GenJavadocPlugin)
  .settings(
    libraryDependencies ++= Dependencies.Commons
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
  .dependsOn(`csw-location`, `csw-config-client`, `csw-config-server` % "test->test", `csw-logging`, `csw-messages`)
  .enablePlugins(DeployApp)
  .settings(
    libraryDependencies ++= Dependencies.Examples
  )
