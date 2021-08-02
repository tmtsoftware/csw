import Common._
import org.tmt.sbt.docs.{Settings => DocSettings}
import sbtcrossproject.CrossPlugin.autoImport.{CrossType, crossProject}

inThisBuild(
  CommonSettings
)

lazy val aggregatedProjects: Seq[ProjectReference] = Seq(
  `csw-prefix`.jvm,
  `csw-prefix`.js,
  `csw-location`,
  `csw-config`,
  `csw-logging`,
  `csw-params`.jvm,
  `csw-params`.js,
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
  `integration`,
  `csw-contract`,
  `csw-services`
)

lazy val unidocExclusions: Seq[ProjectReference] = Seq(
  `csw-location-server`,
  `csw-config-server`,
  `csw-location-agent`,
  `csw-config-cli`,
  `csw-event-cli`,
  `csw-alarm-cli`,
  `csw-time-core`.js,
  `csw-time-clock`.js,
  `csw-logging-macros`,
  `csw-logging-models`.js,
  `csw-params`.js,
  `csw-prefix`.js,
  `csw-command-api`.js,
  `csw-location-api`.js,
  `csw-alarm-models`.js,
  `csw-config-models`.js,
  `csw-network-utils`,
  `csw-commons`,
  `csw-benchmark`,
  `romaine`,
  `examples`,
  `integration`,
  `csw-contract`,
  `csw-services`
)

lazy val githubReleases: Seq[ProjectReference] = Seq(
  `csw-location-server`,
  `csw-location-agent`,
  `csw-config-server`,
  `csw-config-cli`,
  `csw-event-cli`,
  `csw-alarm-cli`
)

lazy val multiJvmProjects: Seq[ProjectReference] = Seq(
  `integration`
)

/* ================= Root Project ============== */
lazy val `csw` = project
  .in(file("."))
  .enablePlugins(NoPublish, UnidocSitePlugin, GithubPublishPlugin, GitBranchPrompt, GithubRelease, ContractPlugin)
  .aggregate(aggregatedProjects: _*)
  .settings(DocSettings.makeSiteMappings(docs))
  .settings(Settings.addAliases)
  .settings(DocSettings.docExclusions(unidocExclusions))
  .settings(Settings.multiJvmTestTask(multiJvmProjects))
  .settings(GithubRelease.githubReleases)
  .settings(
    ghreleaseRepoOrg := "tmtsoftware",
    ghreleaseRepoName := "csw"
  )
  .settings(ghreleaseAssets := {
    ghreleaseAssets.value ++ Seq(
      Settings.addLoggingAggregator.value
    )
  })
  .settings(
    generateContract := ContractPlugin.generate(`csw-contract`).value
  )

lazy val `csw-contract` = project
  .dependsOn(
    `csw-location-api`.jvm,
    `csw-command-api`.jvm,
    `csw-config-models`.jvm,
    `csw-command-client`,
    `csw-params`.jvm
  )
  .settings(
    libraryDependencies ++= Dependencies.Contract.value
  )

lazy val `csw-prefix` = crossProject(JSPlatform, JVMPlatform)
  .crossType(CrossType.Full)
  .jvmConfigure(_.enablePlugins(GenJavadocPlugin))
  .jsSettings(jsTestArg)
  .settings(fork := false)
  .settings(libraryDependencies ++= Dependencies.Prefix.value)

/* ================= Location Service ============== */

lazy val `csw-location` = project
  .in(file("csw-location"))
  .aggregate(
    `csw-location-api`.jvm,
    `csw-location-api`.js,
    `csw-location-server`,
    `csw-location-client`,
    `csw-location-agent`
  )

lazy val `csw-location-api` = crossProject(JSPlatform, JVMPlatform)
  .crossType(CrossType.Full)
  .in(file("csw-location/csw-location-api"))
  .jvmConfigure(_.dependsOn(`csw-commons`))
  .dependsOn(`csw-prefix`)
  .jvmConfigure(_.enablePlugins(GenJavadocPlugin))
  .jvmConfigure(_.dependsOn(`csw-logging-client`).enablePlugins(MaybeCoverage))
  //  the following setting was required by IntelliJ as it can not handle cross-compiled Akka types
  .jsSettings(SettingKey[Boolean]("ide-skip-project") := true)
  .jsSettings(jsTestArg)
  .settings(libraryDependencies ++= Dependencies.LocationApi.value)
  .settings(fork := false)

lazy val `csw-location-server` = project
  .in(file("csw-location/csw-location-server"))
  .dependsOn(
    `csw-location-api`.jvm,
    `csw-logging-client` % "compile->compile;test->test",
    `csw-aas-http`,
    `csw-network-utils`,
    `csw-location-client` % Test,
    `csw-commons`         % "compile->compile;test->test"
  )
  .enablePlugins(CswBuildInfo, MaybeCoverage)
  .settings(
    libraryDependencies ++= Dependencies.LocationServer.value
  )

lazy val `csw-location-client` = project
  .in(file("csw-location/csw-location-client"))
  .dependsOn(
    `csw-location-api`.jvm,
    `csw-network-utils`
  )
  .enablePlugins(GenJavadocPlugin, MaybeCoverage)
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
  .enablePlugins(CswBuildInfo, MaybeCoverage)
  .settings(
    libraryDependencies ++= Dependencies.LocationAgent.value
  )

/* ================= Config Service ============== */
lazy val `csw-config` = project
  .in(file("csw-config"))
  .aggregate(
    `csw-config-models`.jvm,
    `csw-config-models`.js,
    `csw-config-api`,
    `csw-config-server`,
    `csw-config-client`,
    `csw-config-cli`
  )

lazy val `csw-config-models` = crossProject(JSPlatform, JVMPlatform)
  .crossType(CrossType.Pure)
  .in(file("csw-config/csw-config-models"))
  .jvmConfigure(_.enablePlugins(GenJavadocPlugin))
  .jsSettings(jsTestArg)
  .dependsOn(`csw-params`)
  .settings(fork := false)
  .settings(libraryDependencies ++= Dependencies.ConfigModels.value)

lazy val `csw-config-api` = project
  .in(file("csw-config/csw-config-api"))
  .enablePlugins(GenJavadocPlugin, MaybeCoverage)
  .dependsOn(`csw-logging-api`, `csw-config-models`.jvm, `csw-commons` % "test->test")
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
  .enablePlugins(CswBuildInfo, MaybeCoverage)
  .settings(
    libraryDependencies ++= Dependencies.ConfigServer.value
  )

lazy val `csw-config-client` = project
  .in(file("csw-config/csw-config-client"))
  .dependsOn(
    `csw-config-api`,
    `csw-location-api`.jvm,
    `csw-commons`       % "compile->compile;test->test",
    `csw-config-server` % "test->test"
  )
  .enablePlugins(GenJavadocPlugin, MaybeCoverage)
  .settings(
    libraryDependencies ++= Dependencies.ConfigClient.value
  )

lazy val `csw-config-cli` = project
  .in(file("csw-config/csw-config-cli"))
  .dependsOn(
    `csw-config-client`,
    `csw-location-client`,
    `csw-aas-installed`,
    `csw-config-server` % "test->test",
    `csw-commons`       % "test->test"
  )
  .enablePlugins(CswBuildInfo, MaybeCoverage)
  .settings(
    libraryDependencies ++= Dependencies.ConfigCli.value
  )

/* ============ Logging service ============ */

lazy val `csw-logging` = project
  .in(file("csw-logging"))
  .aggregate(
    `csw-logging-macros`,
    `csw-logging-models`.jvm,
    `csw-logging-models`.js,
    `csw-logging-api`,
    `csw-logging-client`
  )

lazy val `csw-logging-macros` = project
  .in(file("csw-logging/csw-logging-macros"))
  .settings(
    libraryDependencies += Libs.`scala-reflect`
  )

lazy val `csw-logging-models` = crossProject(JSPlatform, JVMPlatform)
  .crossType(CrossType.Pure)
  .in(file("csw-logging/csw-logging-models"))
  .jvmConfigure(_.enablePlugins(GenJavadocPlugin))
  .jsSettings(jsTestArg)
  .settings(fork := false)
  .settings(libraryDependencies ++= Dependencies.LoggingModels.value)

lazy val `csw-logging-api` = project
  .in(file("csw-logging/csw-logging-api"))
  .dependsOn(
    `csw-logging-macros`,
    `csw-logging-models`.jvm
  )
  .settings(
    libraryDependencies += Libs.`enumeratum`.value
  )
  .enablePlugins(GenJavadocPlugin, MaybeCoverage)

lazy val `csw-logging-client` = project
  .in(file("csw-logging/csw-logging-client"))
  .dependsOn(`csw-commons` % "compile->compile;test->test", `csw-logging-macros`, `csw-logging-api`, `csw-prefix`.jvm)
  .enablePlugins(GenJavadocPlugin, MaybeCoverage)
  .settings(
    libraryDependencies ++= Dependencies.LoggingClient.value
  )

/* ================= Params ================ */
lazy val `csw-params` = crossProject(JSPlatform, JVMPlatform)
  .crossType(CrossType.Full)
  .dependsOn(`csw-time-core`, `csw-prefix`)
  .jvmConfigure(_.dependsOn(`csw-commons` % "test->test"))
  .jvmConfigure(_.enablePlugins(GenJavadocPlugin))
  .jsSettings(jsTestArg)
  .settings(
    libraryDependencies ++= Dependencies.Params.value,
    fork := false
  )
  .jsSettings(
    libraryDependencies += Libs.`scala-java-time`.value
  )
  .jvmSettings(
    libraryDependencies ++= Dependencies.ParamsJvm.value
  )

/* ================= Framework Project ============== */
lazy val `csw-framework` = project
  .dependsOn(
    `csw-params`.jvm,
    `csw-config-client`,
    `csw-logging-client`,
    `csw-command-client`,
    `csw-event-client`,
    `csw-alarm-client`,
    `csw-time-scheduler`,
    `csw-location-client`,
    `csw-event-client`    % "test->test",
    `csw-location-server` % "test->test",
    `csw-commons`         % "test->test"
  )
  .enablePlugins(GenJavadocPlugin, CswBuildInfo, MaybeCoverage)
  .settings(
    libraryDependencies ++= Dependencies.Framework.value
  )

/* ================= Command Service ============== */
lazy val `csw-command` = project
  .in(file("csw-command"))
  .aggregate(
    `csw-command-api`.jvm,
    `csw-command-api`.js,
    `csw-command-client`
  )

lazy val `csw-command-api` = crossProject(JSPlatform, JVMPlatform)
  .crossType(CrossType.Full)
  .in(file("csw-command/csw-command-api"))
  .dependsOn(
    `csw-params`,
    `csw-location-api`
  )
  .jvmConfigure(_.enablePlugins(GenJavadocPlugin))
  .settings(libraryDependencies ++= Dependencies.CommandApi.value)
  //  the following setting was required by IntelliJ as it can not handle cross-compiled Akka types
  .jsSettings(SettingKey[Boolean]("ide-skip-project") := true)
  .jsSettings(jsTestArg)
  .settings(fork := false)

lazy val `csw-command-client` = project
  .in(file("csw-command/csw-command-client"))
  .dependsOn(
    `csw-command-api`.jvm,
    `csw-logging-client`,
    `csw-aas-http`,
    `csw-location-api`.jvm,
    `csw-location-client` % "test->test",
    `csw-location-server` % "test->test",
    `csw-commons`         % "compile->compile;test->test"
  )
  .enablePlugins(GenJavadocPlugin, MaybeCoverage)
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
  .dependsOn(`csw-params`.jvm)
  .enablePlugins(GenJavadocPlugin)
  .settings(libraryDependencies ++= Dependencies.EventApi.value)

lazy val `csw-event-client` = project
  .in(file("csw-event/csw-event-client"))
  .dependsOn(
    `csw-event-api`,
    `csw-logging-client`,
    `romaine`,
    `csw-location-api`.jvm,
    `csw-location-server` % "test->test",
    `csw-commons`         % "test->test"
  )
  .enablePlugins(GenJavadocPlugin, AutoMultiJvm, MaybeCoverage)
  .settings(libraryDependencies ++= Dependencies.EventClient.value)

lazy val `csw-event-cli` = project
  .in(file("csw-event/csw-event-cli"))
  .dependsOn(
    `csw-location-client`,
    `csw-event-client`  % "compile->compile;test->test",
    `csw-commons`       % "test->test",
    `csw-config-server` % "test->test"
  )
  .enablePlugins(CswBuildInfo, MaybeCoverage)
  .settings(libraryDependencies ++= Dependencies.EventCli.value)

/* ================= Alarm Service ============== */
lazy val `csw-alarm` = project
  .in(file("csw-alarm"))
  .aggregate(
    `csw-alarm-models`.jvm,
    `csw-alarm-models`.js,
    `csw-alarm-api`,
    `csw-alarm-client`,
    `csw-alarm-cli`
  )

lazy val `csw-alarm-models` = crossProject(JSPlatform, JVMPlatform)
  .crossType(CrossType.Pure)
  .in(file("csw-alarm/csw-alarm-models"))
  .dependsOn(`csw-prefix`, `csw-time-core`)
  .jvmConfigure(_.enablePlugins(GenJavadocPlugin))
  .jvmConfigure(_.enablePlugins(MaybeCoverage))
  .jsSettings(jsTestArg)
  .settings(fork := false)
  .settings(libraryDependencies ++= Dependencies.AlarmModels.value)

lazy val `csw-alarm-api` = project
  .in(file("csw-alarm/csw-alarm-api"))
  .dependsOn(`csw-alarm-models`.jvm)
  .enablePlugins(GenJavadocPlugin)
  .settings(libraryDependencies ++= Dependencies.AlarmApi.value)

lazy val `csw-alarm-client` = project
  .in(file("csw-alarm/csw-alarm-client"))
  .dependsOn(
    `csw-alarm-api`,
    `csw-location-api`.jvm,
    `csw-logging-client`,
    `romaine`,
    `csw-logging-client`  % "test->test",
    `csw-commons`         % "test->test",
    `csw-location-server` % "test->compile;test->test"
  )
  .enablePlugins(GenJavadocPlugin, MaybeCoverage)
  .settings(libraryDependencies ++= Dependencies.AlarmClient.value)

lazy val `csw-alarm-cli` = project
  .in(file("csw-alarm/csw-alarm-cli"))
  .dependsOn(
    `csw-alarm-client`,
    `csw-config-client`,
    `csw-location-client`,
    `csw-location-server` % "test->test",
    `csw-config-server`   % "test->test",
    `csw-commons`         % "test->test"
  )
  .enablePlugins(CswBuildInfo, MaybeCoverage)
  .settings(libraryDependencies ++= Dependencies.AlarmCli.value)

/* ================= Time Service ============== */

lazy val `csw-time` = project
  .in(file("csw-time"))
  .aggregate(
    `csw-time-clock`.jvm,
    `csw-time-clock`.js,
    `csw-time-core`.jvm,
    `csw-time-core`.js,
    `csw-time-scheduler`
  )
  .settings(
    test / aggregate := !sys.props.contains("disableTimeTests")
  )

lazy val `csw-time-clock` = crossProject(JSPlatform, JVMPlatform)
  .crossType(CrossType.Dummy)
  .in(file("csw-time/csw-time-clock"))
  .jvmConfigure(_.enablePlugins(GenJavadocPlugin))
  .jvmSettings(libraryDependencies ++= Dependencies.TimeClockJvm.value)
  .jsSettings(libraryDependencies += Libs.`scala-java-time`.value)
  .jsSettings(jsTestArg)
  .settings(fork := false)

lazy val `csw-time-core` = crossProject(JSPlatform, JVMPlatform)
  .crossType(CrossType.Full)
  .in(file("csw-time/csw-time-core"))
  .dependsOn(`csw-time-clock`)
  .jvmConfigure(_.enablePlugins(GenJavadocPlugin))
  .jsSettings(jsTestArg)
  .jvmSettings(libraryDependencies += Libs.`junit-4-13` % Test)
  .settings(
    libraryDependencies += Libs.`scalatest`.value % Test,
    fork := false
  )

lazy val `csw-time-scheduler` = project
  .in(file("csw-time/csw-time-scheduler"))
  .dependsOn(
    `csw-time-core`.jvm % "compile->compile;test->test",
    `csw-logging-client`,
    `csw-commons` % "test->test"
  )
  .enablePlugins(GenJavadocPlugin, MaybeCoverage)
  .settings(libraryDependencies ++= Dependencies.TimeScheduler.value)

lazy val `csw-testkit` = project
  .dependsOn(
    `csw-location-server`,
    `csw-config-server`,
    `csw-framework`,
    `csw-commons` % "test->test"
  )
  .enablePlugins(GenJavadocPlugin)
  .settings(
    libraryDependencies ++= Dependencies.Testkit.value
  )

/* ================= Database Service ============== */

lazy val `csw-database` = project
  .dependsOn(
    `csw-location-api`.jvm,
    `csw-commons`         % "test->test",
    `csw-location-server` % "test->compile;test->test"
  )
  .enablePlugins(GenJavadocPlugin, MaybeCoverage)
  .settings(libraryDependencies ++= Dependencies.DatabaseClient.value)

/* =============== Common Utilities ============ */
lazy val `csw-network-utils` = project
  .enablePlugins(MaybeCoverage)
  .settings(
    libraryDependencies ++= Dependencies.NetworkUtils.value
  )

lazy val `csw-commons` = project
  .settings(
    libraryDependencies ++= Dependencies.Commons.value
  )

/* ==== Lettuce(Redis Driver) Scala Wrapper ==== */
lazy val `romaine` = project
  .settings(
    libraryDependencies ++= Dependencies.Romaine.value
  )

/* ================= Paradox Docs ============== */
lazy val docs = project
  .enablePlugins(NoPublish, ParadoxMaterialSitePlugin)
  .settings(
    paradoxRoots := List(
      "index.html",
      "services/aas/core-concepts-and-terms.html",
      "migration_guide/migration_guide_1.0.0_to_2.0.0/prefix.html",
      "migration_guide/migration_guide_1.0.0_to_2.0.0/commandService.html",
      "services/aas/csw-aas-http.html",
      "services/aas/csw-aas-installed.html",
      "technical/aas/csw-aas-http.html",
      "technical/aas/csw-aas-installed.html",
      "technical/location/location-agent.html",
      "technical/location/location-api.html",
      "technical/location/location-client.html",
      "technical/location/location-server.html"
    )
  )
/* =================== Examples ================ */
lazy val examples = project
  .dependsOn(
    `csw-location-server`,
    `csw-config-client`,
    `csw-aas-http`,
    `csw-logging-client` % "compile->compile;test->test",
    `csw-params`.jvm,
    `csw-database`,
    `csw-framework`,
    `csw-aas-installed`,
    `csw-location-client`,
    `csw-time-scheduler`,
    `csw-time-core`.jvm,
    `csw-testkit`       % "test->compile",
    `csw-config-server` % "test->test"
  )
  .enablePlugins(CswBuildInfo)
  .settings(
    libraryDependencies ++= Dependencies.Examples.value,
    scalacOptions ++= Seq("-Xlint:-unused,-inaccessible", "-Ywarn-dead-code:false")
  )

/* ================ Jmh Benchmarks ============== */
lazy val `csw-benchmark` = project
  .dependsOn(
    `csw-logging-client`,
    `csw-params`.jvm,
    `csw-command-client`,
    `csw-time-scheduler`,
    `csw-location-server` % "compile->test",
    `csw-framework`       % "compile->compile;test->test"
  )
  .enablePlugins(NoPublish, JmhPlugin)
  .settings(
    libraryDependencies ++= Dependencies.Benchmark.value
  )

/* ================ Integration Tests ============= */
lazy val integration = project
  .dependsOn(
    `csw-location-server`,
    `csw-command-client`,
    `csw-location-agent`,
    `csw-network-utils`,
    `csw-aas-installed`,
    `csw-config-cli`,
    `csw-event-api`,
    `csw-event-cli`,
    `csw-framework`     % "multi-jvm->test",
    `csw-commons`       % "multi-jvm->test",
    `csw-config-server` % "multi-jvm->test"
  )
  .enablePlugins(AutoMultiJvm, CswBuildInfo)
  .settings(
    libraryDependencies ++= Dependencies.Integration.value
  )

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
  .dependsOn(`csw-logging-client`, `csw-location-api`.jvm)
  .settings(
    libraryDependencies ++= Dependencies.CswAasCore.value
  )

lazy val `csw-aas-http` = project
  .in(file("csw-aas/csw-aas-http"))
  .dependsOn(
    `csw-aas-core`,
    `csw-commons` % "test->test"
  )
  .settings(
    libraryDependencies ++= Dependencies.AuthAkkaHttpAdapter.value
  )

lazy val `csw-aas-installed` = project
  .in(file("csw-aas/csw-aas-installed"))
  .dependsOn(`csw-aas-core`, `csw-location-client` % "test->compile")
  .settings(
    libraryDependencies ++= Dependencies.CswInstalledAdapter.value
  )

lazy val `csw-services` = project
  .in(file("csw-services"))
  .enablePlugins(CswBuildInfo)
  .dependsOn(`csw-location-server`, `csw-config-server`, `csw-location-agent`, `csw-commons`)
  .settings(
    libraryDependencies ++= Dependencies.CswServices.value
  )
