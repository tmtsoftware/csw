import sbtcrossproject.CrossPlugin.autoImport.{crossProject, CrossType}

val enableCoverage         = sys.props.get("enableCoverage").contains("true")
val MaybeCoverage: Plugins = if (enableCoverage) Coverage else Plugins.empty

lazy val aggregatedProjects: Seq[ProjectReference] = Seq(
  `csw-admin-server`,
  `csw-location-api`,
  `csw-location-server`,
  `csw-location-client`,
  `csw-location-agent`,
  `csw-config-api`,
  `csw-config-server`,
  `csw-config-client`,
  `csw-config-cli`,
  `csw-logging`,
  `csw-logging-macros`,
  `csw-params-jvm`,
  `csw-params-js`,
  `csw-framework`,
  `csw-command-api`,
  `csw-command-client`,
  `csw-event-api`,
  `csw-event-client`,
  `csw-event-cli`,
  `csw-alarm-api`,
  `csw-alarm-client`,
  `csw-alarm-cli`,
  `csw-time-api`,
  `csw-time-client`,
  `csw-database-api`,
  `csw-database-client`,
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
  `csw-time-api`,
  `csw-time-client`,
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
lazy val `csw-location-api` = project
  .dependsOn(
    `csw-logging`,
    `csw-params-jvm`
  )
  .enablePlugins(PublishBintray, GenJavadocPlugin, MaybeCoverage)
  .settings(
    libraryDependencies ++= Dependencies.LocationApi.value
  )

lazy val `csw-location-server` = project
  .dependsOn(
    `csw-location-api`,
    `csw-logging`,
    `csw-network-utils`,
    `csw-location-client` % "test->compile;multi-jvm->compile",
    `csw-commons`         % "compile->compile;test->test"
  )
  .enablePlugins(DeployApp, AutoMultiJvm, MaybeCoverage)
  .settings(
    libraryDependencies ++= Dependencies.LocationServer.value
  )

lazy val `csw-location-client` = project
  .dependsOn(
    `csw-location-api`,
    `csw-network-utils`
  )
  .enablePlugins(PublishBintray, GenJavadocPlugin, MaybeCoverage)
  .settings(
    libraryDependencies ++= Dependencies.LocationClient.value
  )

lazy val `csw-location-agent` = project
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
lazy val `csw-config-api` = project
  .enablePlugins(PublishBintray, GenJavadocPlugin, MaybeCoverage)
  .settings(
    libraryDependencies ++= Dependencies.ConfigApi.value
  )

lazy val `csw-config-server` = project
  .dependsOn(
    `csw-config-api`,
    `csw-location-client`,
    `akka-http-adapter`,
    `csw-location-server` % "test->test",
    `csw-commons`         % "compile->compile;test->test"
  )
  .enablePlugins(DeployApp, MaybeCoverage)
  .settings(
    libraryDependencies ++= Dependencies.ConfigServer.value
  )

lazy val `csw-config-client` = project
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
  .dependsOn(
    `csw-config-client`,
    `csw-location-client`,
    `native-client-adapter`,
    `csw-location-server` % "multi-jvm->multi-jvm",
    `csw-config-server`   % "test->test;multi-jvm->test",
    `csw-commons`         % "test->test"
  )
  .enablePlugins(DeployApp, AutoMultiJvm, MaybeCoverage)
  .settings(
    libraryDependencies ++= Dependencies.ConfigCli.value
  )

/* ============ Logging service ============ */
lazy val `csw-logging-macros` = project
  .settings(
    libraryDependencies += Libs.`scala-reflect`
  )

lazy val `csw-logging` = project
  .dependsOn(`csw-logging-macros`)
  .enablePlugins(PublishBintray, GenJavadocPlugin, MaybeCoverage)
  .settings(
    libraryDependencies ++= Dependencies.Logging.value
  )

/* ================= Params ================ */
lazy val `csw-params` = crossProject(JSPlatform, JVMPlatform)
  .crossType(CrossType.Full)
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
    `csw-logging`,
    `csw-command-client`,
    `csw-event-api`,
    `csw-event-client`,
    `csw-alarm-client`,
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
lazy val `csw-command-api` = project
  .dependsOn(
    `csw-params-jvm`,
    `csw-location-api`
  )
  .enablePlugins(PublishBintray, GenJavadocPlugin)

lazy val `csw-command-client` = project
  .dependsOn(
    `csw-command-api`,
    `csw-logging`,
    `csw-commons` % "test->test"
  )
  .enablePlugins(PublishBintray, GenJavadocPlugin, AutoMultiJvm, MaybeCoverage)
  .settings(libraryDependencies ++= Dependencies.CommandClient.value)

/* ================= Event Service ============== */
lazy val `csw-event-api` = project
  .dependsOn(`csw-params-jvm`)
  .enablePlugins(PublishBintray, GenJavadocPlugin)
  .settings(libraryDependencies ++= Dependencies.EventApi.value)

lazy val `csw-event-client` = project
  .dependsOn(
    `csw-event-api`,
    `csw-logging`,
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
  .dependsOn(
    `csw-location-client`,
    `csw-event-client`  % "compile->compile;test->test;test->multi-jvm",
    `csw-commons`       % "test->test",
    `csw-config-server` % "test->test"
  )
  .enablePlugins(DeployApp, MaybeCoverage)
  .settings(libraryDependencies ++= Dependencies.EventCli.value)

/* ================= Alarm Service ============== */
lazy val `csw-alarm-api` = project
  .dependsOn(`csw-params-jvm`)
  .enablePlugins(PublishBintray, GenJavadocPlugin)
  .settings(libraryDependencies ++= Dependencies.AlarmApi.value)

lazy val `csw-alarm-client` = project
  .dependsOn(
    `csw-alarm-api`,
    `csw-location-api`,
    `csw-logging`,
    `romaine`,
    `csw-logging`         % "test->test",
    `csw-commons`         % "test->test",
    `csw-location-server` % "test->compile;test->test"
  )
  .enablePlugins(PublishBintray, GenJavadocPlugin, MaybeCoverage)
  .settings(libraryDependencies ++= Dependencies.AlarmClient.value)

lazy val `csw-alarm-cli` = project
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

lazy val `csw-time-api` = project

lazy val Linux = config("linux").extend(Test)

lazy val `csw-time-client` = project
  .dependsOn(`csw-time-api`)
  .configs(Linux)
  .settings(libraryDependencies ++= Dependencies.TimeClient.value)
  .settings(inConfig(Linux)(Defaults.testTasks): _*)
  .settings(testOptions in Linux -= Tests.Argument("-l", "csw.time.client.tags.Linux"))
  .settings(testOptions in Linux ++= Seq(Tests.Argument("-n", "csw.time.client.tags.Linux")))

lazy val `csw-testkit` = project
  .dependsOn(
    `csw-location-server`,
    `csw-config-server`,
    `csw-framework`
  )
  .enablePlugins(PublishBintray, GenJavadocPlugin)
  .settings(libraryDependencies ++= Dependencies.Testkit.value)

/* ================= Database Service ============== */

lazy val `csw-database-api` = project

lazy val `csw-database-client` = project
  .dependsOn(`csw-database-api`, `csw-location-api`)
  .settings(
    parallelExecution in Test := false,
    libraryDependencies ++= Dependencies.DatabaseClient.value
  )

/* =============== Common Utilities ============ */
lazy val `csw-network-utils` = project
  .dependsOn(`csw-logging`)
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
    `csw-logging`,
    `csw-params-jvm`,
    `csw-framework`,
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
    `csw-logging`,
    `csw-params-jvm`,
    `csw-command-client`,
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
lazy val `csw-auth` = project
  .in(file("csw-auth"))
  .aggregate(
    `native-client-adapter`,
    `auth-core`,
    `akka-http-adapter`
  )

lazy val `auth-core` = project
  .in(file("csw-auth/auth-core"))
  .dependsOn(`csw-logging`)
  .settings(
    libraryDependencies ++= Dependencies.AuthAdapterCore.value
  )

lazy val `akka-http-adapter` = project
  .in(file("csw-auth/akka-http-adapter"))
  .dependsOn(`auth-core`)
  .settings(
    libraryDependencies ++= Dependencies.AuthAkkaHttpAdapter.value
  )

 lazy val `native-client-adapter` = project
  .in(file("csw-auth/native-client-adapter"))
  .dependsOn(`auth-core`)
  .settings(
    libraryDependencies ++= Dependencies.AuthNativeClientAdapter.value
  )
