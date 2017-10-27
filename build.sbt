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

//Root project
lazy val `csw-prod` = project
  .in(file("."))
  .enablePlugins(UnidocSite, PublishGithub, GitBranchPrompt)
  .aggregate(aggregatedProjects: _*)
  .settings(Settings.mergeSiteWith(docs))
  .settings(Settings.docExclusions(unidocExclusions))

lazy val `csw-messages` = project
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

lazy val `csw-benchmark` = project
  .dependsOn(`csw-logging`, `csw-messages`)
  .enablePlugins(JmhPlugin)
  .settings(
    libraryDependencies ++= Dependencies.Benchmark
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
    `csw-framework`     % "test->test",
    `csw-config-server` % "test->test",
  )
  .enablePlugins(DeployApp, MaybeCoverage)
  .settings(
    libraryDependencies ++= Dependencies.CswClusterSeed
  )

lazy val `csw-location-agent` = project
  .dependsOn(`csw-location`)
  .enablePlugins(DeployApp, MaybeCoverage)
  .settings(
    libraryDependencies ++= Dependencies.CswLocationAgent
  )

//Config service related projects
lazy val `csw-config-api` = project
  .enablePlugins(GenJavadocPlugin, MaybeCoverage)
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
    `csw-config-server` % "test->test",
    `csw-location`      % "compile->compile;multi-jvm->multi-jvm"
  )
  .enablePlugins(AutoMultiJvm, MaybeCoverage)
  .settings(
    libraryDependencies ++= Dependencies.ConfigClient
  )

lazy val `csw-config-client-cli` = project
  .dependsOn(
    `csw-config-client`,
    `csw-config-server` % "test->test",
    `csw-location`      % "multi-jvm->multi-jvm"
  )
  .enablePlugins(AutoMultiJvm, DeployApp, MaybeCoverage)
  .settings(
    libraryDependencies ++= Dependencies.CswConfigClientCli
  )

lazy val `csw-vslice` = project
  .dependsOn(`csw-framework`)

lazy val `csw-framework` = project
  .dependsOn(
    `csw-messages`,
    `csw-config-client`,
    `csw-logging`,
    `csw-location`      % "compile->compile;multi-jvm->multi-jvm",
    `csw-config-server` % "multi-jvm->test"
  )
  .enablePlugins(AutoMultiJvm, GenJavadocPlugin, CswBuildInfo)
  .settings(
    libraryDependencies ++= Dependencies.CswFramework
  )

lazy val `csw-commons` = project
  .enablePlugins(PublishBintray, GenJavadocPlugin)
  .settings(
    libraryDependencies ++= Dependencies.CswCommons
  )

//Integration test project
lazy val integration = project
  .dependsOn(`csw-location`, `csw-location-agent`)
  .enablePlugins(DeployApp)
  .settings(
    libraryDependencies ++= Dependencies.Integration
  )

//Docs project
lazy val docs = project.enablePlugins(ParadoxSite, NoPublish)

//Example code
lazy val examples = project
  .dependsOn(`csw-location`, `csw-config-client`, `csw-config-server` % "test->test", `csw-logging`)
  .enablePlugins(DeployApp)
  .settings(
    libraryDependencies ++= Dependencies.CswProdExamples
  )
