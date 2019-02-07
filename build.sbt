import sbtcrossproject.CrossPlugin.autoImport.{CrossType, crossProject}

val enableCoverage         = sys.props.get("enableCoverage").contains("true")
val MaybeCoverage: Plugins = if (enableCoverage) Coverage else Plugins.empty

lazy val aggregatedProjects: Seq[ProjectReference] = Seq(
  `csw-admin-server`,
  `csw-location`,
  `csw-config`,
  `csw-logging`,
  `csw-params-jvm`,
  `csw-params-js`,
  `csw-framework`,
  `csw-command`,
  `csw-event`,
  `csw-alarm`,
  `csw-aas`,
  `csw-time`,
  `csw-database`,
  `csw-network-utils`,
  `csw-commons`,
  `csw-testkit`,
  `csw-benchmark`,
  `romaine`,
  `examples`,
  `integration`
)

lazy val unidocExclusions: Seq[ProjectReference] = Seq(
  `csw-admin-server`,
  `csw-location-server`,
  `csw-config-server`,
  `csw-location-agent`,
  `csw-config-cli`,
  `csw-event-cli`,
  `csw-alarm-cli`,
  `csw-time-api-js`,
  `csw-time-clock-js`,
  `csw-logging-macros`,
  `csw-params-js`,
  `csw-network-utils`,
  `csw-commons`,
  `csw-benchmark`,
  `romaine`,
  `examples`,
  `integration`
)

lazy val githubReleases: Seq[ProjectReference] = Seq(
  `csw-admin-server`,
  `csw-location-server`,
  `csw-location-agent`,
  `csw-config-server`,
  `csw-config-cli`,
  `csw-event-cli`,
  `csw-alarm-cli`
)

/* ================= Root Project ============== */
lazy val `csw` = project
  .in(file("."))
  .enablePlugins(NoPublish, UnidocSite, GithubPublishDocs, GitBranchPrompt, GithubRelease)
  .disablePlugins(BintrayPlugin)
  .aggregate(aggregatedProjects: _*)
  .settings(Settings.mergeSiteWith(docs))
  .settings(Settings.docExclusions(unidocExclusions))
  .settings(GithubRelease.githubReleases(githubReleases))

/* ================= Admin Project ============== */
lazy val `csw-admin-server` = project
  .dependsOn(
    `csw-location-client`,
    `csw-command-client`,
    `csw-commons`       % "compile->compile;test->test",
    `csw-framework`     % "test->test",
    `csw-config-server` % "test->test"
  )
  .enablePlugins(DeployApp, MaybeCoverage)
  .settings(
    libraryDependencies ++= Dependencies.AdminServer.value
  )

/* ================= Location Service ============== */

lazy val `csw-location` = project
  .in(file("csw-location"))
  .aggregate(
    `csw-location-api`,
    `csw-location-server`,
    `csw-location-client`,
    `csw-location-agent`
  )

lazy val `csw-location-api` = project
  .in(file("csw-location/csw-location-api"))
  .dependsOn(
    `csw-logging-client`,
    `csw-params-jvm`
  )
  .enablePlugins(PublishBintray, GenJavadocPlugin, MaybeCoverage)
  .settings(
    libraryDependencies ++= Dependencies.LocationApi.value
  )

lazy val `csw-location-server` = project
  .in(file("csw-location/csw-location-server"))
  .dependsOn(
    `csw-location-api`,
    `csw-logging-client`,
    `csw-network-utils`,
    `csw-location-client` % "test->compile;multi-jvm->compile",
    `csw-commons`         % "compile->compile;test->test"
  )
  .enablePlugins(DeployApp, AutoMultiJvm, MaybeCoverage)
  .settings(
    libraryDependencies ++= Dependencies.LocationServer.value
  )

lazy val `csw-location-client` = project
  .in(file("csw-location/csw-location-client"))
  .dependsOn(
    `csw-location-api`,
    `csw-network-utils`
  )
  .enablePlugins(PublishBintray, GenJavadocPlugin, MaybeCoverage)
  .settings(
    libraryDependencies ++= Dependencies.LocationClient.value
  )

lazy val `csw-location-agent` = project
  .in(file("csw-location/csw-location-agent"))
  .dependsOn(
    `csw-location-client`,
    `csw-commons` % "test->test",
    `csw-testkit` % "test->compile"
  )
  .enablePlugins(DeployApp, MaybeCoverage)
  .settings(
    libraryDependencies ++= Dependencies.LocationAgent.value
  )

/* ================= Config Service ============== */
lazy val `csw-config` = project
  .in(file("csw-config"))
  .aggregate(
    `csw-config-api`,
    `csw-config-server`,
    `csw-config-client`,
    `csw-config-cli`
  )

lazy val `csw-config-api` = project
  .in(file("csw-config/csw-config-api"))
  .enablePlugins(PublishBintray, GenJavadocPlugin, MaybeCoverage)
  .settings(
    libraryDependencies ++= Dependencies.ConfigApi.value
  )

lazy val `csw-config-server` = project
  .in(file("csw-config/csw-config-server"))
  .dependsOn(
    `csw-config-api`,
    `csw-location-client`,
    `csw-aas-http`,
    `csw-location-server` % "test->test",
    `csw-commons`         % "compile->compile;test->test"
  )
  .enablePlugins(DeployApp, MaybeCoverage)
  .settings(
    libraryDependencies ++= Dependencies.ConfigServer.value
  )

lazy val `csw-config-client` = project
  .in(file("csw-config/csw-config-client"))
  .dependsOn(
    `csw-config-api`,
    `csw-location-api`,
    `csw-commons`         % "compile->compile;test->test",
    `csw-location-server` % "multi-jvm->multi-jvm",
    `csw-config-server`   % "test->test;multi-jvm->test"
  )
  .enablePlugins(PublishBintray, GenJavadocPlugin, AutoMultiJvm, MaybeCoverage)
  .settings(
    libraryDependencies ++= Dependencies.ConfigClient.value
  )

lazy val `csw-config-cli` = project
  .in(file("csw-config/csw-config-cli"))
  .dependsOn(
    `csw-config-client`,
    `csw-location-client`,
    `csw-aas-installed`,
    `csw-location-server` % "multi-jvm->multi-jvm",
    `csw-config-server`   % "test->test;multi-jvm->test",
    `csw-commons`         % "test->test"
  )
  .enablePlugins(DeployApp, AutoMultiJvm, MaybeCoverage)
  .settings(
    libraryDependencies ++= Dependencies.ConfigCli.value
  )

/* ============ Logging service ============ */

lazy val `csw-logging` = project
  .in(file("csw-logging"))
  .aggregate(
    `csw-logging-macros`,
    `csw-logging-api`,
    `csw-logging-client`,
  )

lazy val `csw-logging-macros` = project
  .in(file("csw-logging/csw-logging-macros"))
  .settings(
    libraryDependencies += Libs.`scala-reflect`
  )

lazy val `csw-logging-api` = project
  .in(file("csw-logging/csw-logging-api"))
  .dependsOn(`csw-logging-macros`)
  .enablePlugins(PublishBintray, GenJavadocPlugin, MaybeCoverage)

lazy val `csw-logging-client` = project
  .in(file("csw-logging/csw-logging-client"))
  .dependsOn(`csw-logging-macros`, `csw-logging-api`)
  .enablePlugins(PublishBintray, GenJavadocPlugin, MaybeCoverage)
  .settings(
    libraryDependencies ++= Dependencies.LoggingClient.value
  )


/* ================= Params ================ */
lazy val `csw-params` = crossProject(JSPlatform, JVMPlatform)
  .crossType(CrossType.Full)
  .dependsOn(`csw-time-api`)
  .enablePlugins(PublishBintray, GenJavadocPlugin)
  .settings(
    libraryDependencies ++= Dependencies.Params.value,
    Common.detectCycles := false,
    fork := false
  )

lazy val `csw-params-js` = `csw-params`.js
  .settings(
    libraryDependencies += Libs.`scalajs-java-time`.value
  )

lazy val `csw-params-jvm` = `csw-params`.jvm
  .dependsOn(`csw-commons` % "test->test")
  .settings(
    libraryDependencies ++= Dependencies.ParamsJvm.value
  )

/* ================= Framework Project ============== */
lazy val `csw-framework` = project
  .dependsOn(
    `csw-params-jvm`,
    `csw-config-client`,
    `csw-logging-client`,
    `csw-command-client`,
    `csw-event-client`,
    `csw-alarm-client`,
    `csw-time-client`,
    `csw-location-client`,
    `csw-event-client`    % "test->test",
    `csw-location-server` % "test->test;multi-jvm->multi-jvm",
    `csw-config-server`   % "multi-jvm->test",
    `csw-commons`         % "test->test"
  )
  .enablePlugins(PublishBintray, GenJavadocPlugin, AutoMultiJvm, CswBuildInfo, MaybeCoverage)
  .settings(
    libraryDependencies ++= Dependencies.Framework.value
  )

/* ================= Command Service ============== */
lazy val `csw-command` = project
  .in(file("csw-command"))
  .aggregate(
    `csw-command-api`,
    `csw-command-client`,
  )


lazy val `csw-command-api` = project
  .in(file("csw-command/csw-command-api"))
  .dependsOn(
    `csw-params-jvm`,
    `csw-location-api`
  )
  .enablePlugins(PublishBintray, GenJavadocPlugin)
  .settings(libraryDependencies ++= Dependencies.CommandApi.value)

lazy val `csw-command-client` = project
  .in(file("csw-command/csw-command-client"))
  .dependsOn(
    `csw-command-api`,
    `csw-logging-client`,
    `csw-commons` % "test->test"
  )
  .enablePlugins(PublishBintray, GenJavadocPlugin, AutoMultiJvm, MaybeCoverage)
  .settings(libraryDependencies ++= Dependencies.CommandClient.value)

/* ================= Event Service ============== */

lazy val `csw-event` = project
  .in(file("csw-event"))
  .aggregate(
    `csw-event-api`,
    `csw-event-client`,
    `csw-event-cli`
  )

lazy val `csw-event-api` = project
  .in(file("csw-event/csw-event-api"))
  .dependsOn(`csw-params-jvm`)
  .enablePlugins(PublishBintray, GenJavadocPlugin)
  .settings(libraryDependencies ++= Dependencies.EventApi.value)

lazy val `csw-event-client` = project
  .in(file("csw-event/csw-event-client"))
  .dependsOn(
    `csw-event-api`,
    `csw-logging-client`,
    `romaine`,
    `csw-location-api`,
    `csw-location-server` % "test->test;multi-jvm->multi-jvm",
    `csw-commons`         % "test->test"
  )
  .enablePlugins(PublishBintray, GenJavadocPlugin, AutoMultiJvm, MaybeCoverage)
  .settings(libraryDependencies ++= Dependencies.EventClient.value)
  .settings(
    Common.detectCycles := false,
    PB.targets in Compile := Seq(
      PB.gens.java                        -> (sourceManaged in Compile).value,
      scalapb.gen(javaConversions = true) -> (sourceManaged in Compile).value
    )
  )

lazy val `csw-event-cli` = project
  .in(file("csw-event/csw-event-cli"))
  .dependsOn(
    `csw-location-client`,
    `csw-event-client`  % "compile->compile;test->test;test->multi-jvm",
    `csw-commons`       % "test->test",
    `csw-config-server` % "test->test"
  )
  .enablePlugins(DeployApp, MaybeCoverage)
  .settings(libraryDependencies ++= Dependencies.EventCli.value)

/* ================= Alarm Service ============== */
lazy val `csw-alarm` = project
  .in(file("csw-alarm"))
  .aggregate(
    `csw-alarm-api`,
    `csw-alarm-client`,
    `csw-alarm-cli`
  )


lazy val `csw-alarm-api` = project
  .in(file("csw-alarm/csw-alarm-api"))
  .dependsOn(`csw-params-jvm`, `csw-time-api-jvm`)
  .enablePlugins(PublishBintray, GenJavadocPlugin)
  .settings(libraryDependencies ++= Dependencies.AlarmApi.value)

lazy val `csw-alarm-client` = project
  .in(file("csw-alarm/csw-alarm-client"))
  .dependsOn(
    `csw-alarm-api`,
    `csw-location-api`,
    `csw-logging-client`,
    `romaine`,
    `csw-logging-client`         % "test->test",
    `csw-commons`         % "test->test",
    `csw-location-server` % "test->compile;test->test"
  )
  .enablePlugins(PublishBintray, GenJavadocPlugin, MaybeCoverage)
  .settings(libraryDependencies ++= Dependencies.AlarmClient.value)

lazy val `csw-alarm-cli` = project
  .in(file("csw-alarm/csw-alarm-cli"))
  .dependsOn(
    `csw-alarm-client`,
    `csw-config-client`,
    `csw-location-client`,
    `csw-location-server` % "test->test",
    `csw-config-server`   % "test->test"
  )
  .enablePlugins(DeployApp, MaybeCoverage)
  .settings(libraryDependencies ++= Dependencies.AlarmCli.value)

/* ================= Time Service ============== */

lazy val `csw-time` = project
  .in(file("csw-time"))
  .aggregate(
    `csw-time-clock-jvm`,
    `csw-time-clock-js`,
    `csw-time-api-jvm`,
    `csw-time-api-js`,
    `csw-time-client`
  )

lazy val `csw-time-clock` = crossProject(JSPlatform, JVMPlatform)
  .crossType(CrossType.Dummy)
  .in(file("csw-time/csw-time-clock"))
  .enablePlugins(PublishBintray, GenJavadocPlugin)
  .jvmSettings(libraryDependencies ++= Dependencies.TimeClockJvm.value)
  .jsSettings(libraryDependencies += Libs.`scalajs-java-time`.value)
  .settings(fork := false)

lazy val `csw-time-clock-js` = `csw-time-clock`.js
lazy val `csw-time-clock-jvm` = `csw-time-clock`.jvm

lazy val `csw-time-api` =  crossProject(JSPlatform, JVMPlatform)
  .crossType(CrossType.Pure)
  .in(file("csw-time/csw-time-api"))
  .dependsOn(`csw-time-clock`)
  .enablePlugins(PublishBintray, GenJavadocPlugin)
  .settings(
    libraryDependencies ++= Dependencies.TimeApi.value,
    fork := false
  )

lazy val `csw-time-api-js` = `csw-time-api`.js
lazy val `csw-time-api-jvm` = `csw-time-api`.jvm

lazy val `csw-time-client` = project
  .in(file("csw-time/csw-time-client"))
  .dependsOn(
    `csw-time-api-jvm` % "compile->compile;test->test",
    `csw-logging-client`
  )
  .settings(libraryDependencies ++= Dependencies.TimeClient.value)

lazy val `csw-testkit` = project
  .dependsOn(
    `csw-location-server`,
    `csw-config-server`,
    `csw-framework`
  )
  .enablePlugins(PublishBintray, GenJavadocPlugin)
  .settings(libraryDependencies ++= Dependencies.Testkit.value)

/* ================= Database Service ============== */

lazy val `csw-database` = project
  .dependsOn(
    `csw-location-api`,
    `csw-location-server` % "test->compile;test->test"
  )
  .enablePlugins(PublishBintray, GenJavadocPlugin, MaybeCoverage)
  .settings(libraryDependencies ++= Dependencies.DatabaseClient.value)

/* =============== Common Utilities ============ */
lazy val `csw-network-utils` = project
  .dependsOn(`csw-logging-client`)
  .enablePlugins(PublishBintray)
  .settings(
    libraryDependencies ++= Dependencies.NetworkUtils.value
  )

lazy val `csw-commons` = project
  .dependsOn(`csw-network-utils`)
  .enablePlugins(PublishBintray)
  .settings(
    libraryDependencies ++= Dependencies.Commons.value
  )

/* ==== Lettuce(Redis Driver) Scala Wrapper ==== */
lazy val `romaine` = project
  .enablePlugins(PublishBintray)
  .settings(
    libraryDependencies ++= Dependencies.Romaine.value
  )

/* ================= Paradox Docs ============== */
lazy val docs = project.enablePlugins(NoPublish, ParadoxSite)

/* =================== Examples ================ */
lazy val examples = project
  .dependsOn(
    `csw-location-server`,
    `csw-config-client`,
    `csw-aas-http`,
    `csw-logging-client`,
    `csw-params-jvm`,
    `csw-database`,
    `csw-framework`,
    `csw-aas-installed`,
    `csw-location-client`,
    `csw-testkit`       % "test->compile",
    `csw-config-server` % "test->test"
  )
  .enablePlugins(DeployApp)
  .settings(
    libraryDependencies ++= Dependencies.Examples.value
  )

/* ================ Jmh Benchmarks ============== */
lazy val `csw-benchmark` = project
  .dependsOn(
    `csw-logging-client`,
    `csw-params-jvm`,
    `csw-command-client`,
    `csw-time-client`,
    `csw-location-server` % "compile->test",
    `csw-framework`       % "compile->compile;test->test"
  )
  .enablePlugins(NoPublish, JmhPlugin)
  .disablePlugins(BintrayPlugin)
  .settings(
    libraryDependencies ++= Dependencies.Benchmark.value
  )

/* ================ Integration Tests ============= */
lazy val integration = project
  .dependsOn(
    `csw-location-server`,
    `csw-command-client`,
    `csw-location-agent`,
    `csw-network-utils`
  )
  .enablePlugins(NoPublish, DeployApp)
  .settings(
    libraryDependencies ++= Dependencies.Integration.value
  )

/* ================================================ */
lazy val alarm = taskKey[Unit]("alarm")

alarm := {
  (test in (`csw-alarm-client`, Test)).value
  (test in (`csw-alarm-api`, Test)).value
  (test in (`csw-alarm-cli`, Test)).value
}

// ================================================
/* ===================== Auth ================== */
// ================================================
lazy val `csw-aas` = project
  .in(file("csw-aas"))
  .aggregate(
    `csw-aas-core`,
    `csw-aas-http`,
    `csw-aas-installed`
  )

lazy val `csw-aas-core` = project
  .in(file("csw-aas/csw-aas-core"))
  .dependsOn(`csw-logging-client`, `csw-location-api`)
  .settings(
    libraryDependencies ++= Dependencies.CswAasCore.value
  )

lazy val `csw-aas-http` = project
  .in(file("csw-aas/csw-aas-http"))
  .dependsOn(
    `csw-aas-core`,
    `csw-location-server` % "multi-jvm->multi-jvm")
  .enablePlugins(AutoMultiJvm)
  .settings(
    libraryDependencies ++= Dependencies.AuthAkkaHttpAdapter.value
  )

 lazy val `csw-aas-installed` = project
  .in(file("csw-aas/csw-aas-installed"))
  .dependsOn(`csw-aas-core`, `csw-location-client` % "test->compile")
  .settings(
    libraryDependencies ++= Dependencies.CswInstalledAdapter.value
  )

lazy val `csw-aas-react4s-example` = project
  .in(file("csw-aas/csw-aas-react4s-example"))
  .enablePlugins(ScalaJSBundlerPlugin)
  .dependsOn(`csw-aas-react4s-facade`)
  .settings(
    scalaJSUseMainModuleInitializer := true,
    fork := false,
    scalacOptions += "-P:scalajs:sjsDefinedByDefault",
    resolvers += Resolver.sonatypeRepo("snapshots"),
    libraryDependencies ++= Dependencies.AASReact4s.value,
    npmDependencies in Compile ++= Dependencies.AASReact4sNpmDeps,
    npmDevDependencies in Compile ++= Dependencies.AASReact4sNpmDevDeps,
    version in webpack := "4.28.4",
    version in startWebpackDevServer := "3.1.14",
    webpackConfigFile := Some(baseDirectory.value / "aas.webpack.config.js"),
    webpackResources := webpackResources.value +++ PathFinder(Seq(baseDirectory.value / "index.html")) ** "*.*",
    webpackDevServerExtraArgs in fastOptJS ++= Seq(
      "--content-base",
      baseDirectory.value.getAbsolutePath
    )
  )

lazy val `csw-aas-react4s-facade` = project
  .in(file("csw-aas/csw-aas-react4s-facade"))
  .enablePlugins(ScalaJSPlugin)
  .settings(
    fork := false,
    scalacOptions += "-P:scalajs:sjsDefinedByDefault",
    resolvers += Resolver.sonatypeRepo("snapshots"),
    libraryDependencies ++= Seq(
      Libs.`scalatest`.value % Test,
      React4s.`react4s`.value,
    )
  )
