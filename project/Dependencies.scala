import sbt._

object Dependencies {

  val AdminServer = Def.setting(
    Seq(
      Libs.`config`,
      Akka.`akka-actor`,
      Akka.`akka-stream`,
      Akka.`akka-stream-typed`,
      Akka.`akka-actor-typed`,
      AkkaHttp.`akka-http`,
      Libs.`scala-async`.value,
      Libs.`play-json`.value,
      Libs.`akka-http-play-json`,
      Libs.`scopt`,
      Libs.`scalatest`.value % Test
    )
  )

  val LocationApi = Def.setting(
    Seq(
      Akka.`akka-actor`,
      Akka.`akka-actor-typed`,
      Akka.`akka-stream`,
      Akka.`akka-stream-typed`,
      Libs.`play-json`.value,
      Libs.`play-json-derived-codecs`.value,
      Enumeratum.`enumeratum`.value,
      Libs.`scalatest`.value % Test,
      Libs.`mockito-scala`   % Test
    )
  )

  val LocationServer = Def.setting(
    Seq(
      Libs.`config`,
      Akka.`akka-actor`,
      Akka.`akka-actor-typed`,
      Akka.`akka-stream`,
      Akka.`akka-stream-typed`,
      Akka.`akka-cluster`,
      Akka.`akka-distributed-data`,
      Akka.`akka-cluster-typed`,
      AkkaHttp.`akka-http-cors`,
      Libs.`scala-async`.value,
      Libs.`scopt`,
      Enumeratum.`enumeratum`.value,
      Libs.`akka-management-cluster-http`,
      AkkaHttp.`akka-http`,
      Libs.`akka-http-play-json`,
      Libs.`play-json`.value,
      Chill.`chill-akka`,
      Akka.`akka-actor-testkit-typed` % Test,
      Libs.`scalatest`.value          % Test,
      Libs.`junit`                    % Test,
      Libs.`junit-interface`          % Test,
      Libs.`mockito-scala`            % Test,
      Akka.`akka-stream-testkit`      % Test,
      Akka.`akka-multi-node-testkit`  % Test
    )
  )

  val LocationClient = Def.setting(
    Seq(
      Libs.`config`,
      Akka.`akka-actor`,
      Akka.`akka-stream`,
      Akka.`akka-stream-typed`,
      AkkaHttp.`akka-http`,
      Akka.`akka-remote`,
      Libs.`scala-async`.value,
      Libs.`scala-java8-compat`,
      Libs.`play-json`.value,
      Libs.`akka-http-play-json`,
      Libs.`scalatest`.value % Test
    )
  )

  val LocationAgent = Def.setting(
    Seq(
      Libs.`config`,
      AkkaHttp.`akka-http`,
      Akka.`akka-actor`,
      Akka.`akka-stream`,
      Akka.`akka-stream-typed`,
      Libs.`scopt`,
      Libs.`scalatest`.value % Test
    )
  )

  val ConfigApi = Def.setting(
    Seq(
      Enumeratum.`enumeratum`.value,
      Akka.`akka-actor`,
      Akka.`akka-stream`,
      Akka.`akka-stream-typed`,
      Libs.`akka-http-play-json`,
      Libs.`config`,
      Libs.`play-json`.value,
      Libs.`scala-java8-compat`,
      Libs.`scalatest`.value     % Test,
      Akka.`akka-stream-testkit` % Test
    )
  )

  val ConfigServer = Def.setting(
    Seq(
      Enumeratum.`enumeratum`.value,
      Akka.`akka-stream`,
      Akka.`akka-stream-typed`,
      Akka.`akka-actor`,
      Libs.`play-json`.value,
      Libs.`akka-http-play-json`,
      Libs.`scala-async`.value,
      Libs.`scala-java8-compat`,
      Libs.`config`,
      AkkaHttp.`akka-http`,
      AkkaHttp.`akka-http-cors`,
      Libs.svnkit,
      Libs.`scopt`,
      Libs.`scalatest`.value       % Test,
      AkkaHttp.`akka-http-testkit` % Test,
      Akka.`akka-stream-testkit`   % Test
    )
  )

  val ConfigClient = Def.setting(
    Seq(
      Libs.`config`,
      Libs.`play-json`.value,
      Akka.`akka-actor`,
      Akka.`akka-stream`,
      Akka.`akka-stream-typed`,
      Libs.`akka-http-play-json`,
      AkkaHttp.`akka-http`,
      Libs.`scala-async`.value,
      Libs.`scala-java8-compat`,
      Libs.`scalatest`.value         % Test,
      Libs.`junit`                   % Test,
      Libs.`junit-interface`         % Test,
      Libs.`mockito-scala`           % Test,
      Akka.`akka-multi-node-testkit` % Test,
      Akka.`akka-stream-testkit`     % Test
    )
  )

  val ConfigCli = Def.setting(
    Seq(
      Libs.`config`,
      AkkaHttp.`akka-http`,
      Akka.`akka-actor`,
      Akka.`akka-stream`,
      Akka.`akka-stream-typed`,
      Libs.`scopt`,
      Libs.`scalatest`.value         % Test,
      Akka.`akka-multi-node-testkit` % Test,
      Libs.`embedded-keycloak`       % Test
    )
  )

  val LoggingClient = Def.setting(
    Seq(
      Libs.`config`,
      Libs.`logback-classic`,
      Libs.`play-json`.value,
      Libs.`scala-java8-compat`,
      Enumeratum.`enumeratum`.value,
      Akka.`akka-actor`,
      Akka.`akka-actor-typed`,
      Libs.`scalatest`.value % Test,
      Libs.`junit`           % Test,
      Libs.`junit-interface` % Test,
      Libs.`gson`            % Test
    )
  )

  val Params = Def.setting(
    Seq(
      Enumeratum.`enumeratum`.value,
      Enumeratum.`enumeratum-play-json`.value,
      Libs.`play-json`.value,
      Libs.`play-json-derived-codecs`.value,
      Libs.`scalatest`.value % Test
    )
  )

  val ParamsJvm = Def.setting(
    Seq(
      Chill.`chill-bijection` % Test,
      Libs.`junit`            % Test,
      Libs.`junit-interface`  % Test
    )
  )

  val Framework = Def.setting(
    Seq(
      Enumeratum.`enumeratum`.value,
      Libs.`config`,
      Akka.`akka-actor`,
      Akka.`akka-stream`,
      Akka.`akka-stream-typed`,
      Libs.`scala-async`.value,
      Libs.`scala-java8-compat`,
      Libs.`play-json`.value,
      Akka.`akka-actor-typed`,
      Libs.`scopt`,
      Akka.`akka-actor-testkit-typed` % Test,
      Akka.`akka-stream-testkit`      % Test,
      Libs.`scalatest`.value          % Test,
      Libs.`junit`                    % Test,
      Libs.`junit-interface`          % Test,
      Libs.`mockito-scala`            % Test
    )
  )

  val CommandClient = Def.setting(
    Seq(
      Enumeratum.`enumeratum`.value,
      Libs.`config`,
      Libs.`play-json`.value,
      Libs.`scala-java8-compat`,
      Akka.`akka-stream`,
      Akka.`akka-stream-typed`,
      Akka.`akka-actor`,
      Akka.`akka-actor-typed`,
      Chill.`chill-akka`,
      Libs.`caffeine`,
      Akka.`akka-actor-testkit-typed` % Test,
      Akka.`akka-stream-testkit`      % Test,
      Libs.`scalatest`.value          % Test,
      Libs.`junit`                    % Test,
      Libs.`junit-interface`          % Test,
      Libs.`mockito-scala`            % Test
    )
  )

  val CommandApi = Def.setting(
    Seq(
      Akka.`akka-actor`
    )
  )

  val EventApi = Def.setting(
    Seq(
      Akka.`akka-actor`,
      Akka.`akka-stream`,
      Akka.`akka-stream-typed`,
      Akka.`akka-actor-typed`,
      Libs.`scala-java8-compat`
    )
  )

  val EventClient = Def.setting(
    Seq(
      Enumeratum.`enumeratum`.value,
      Libs.`config`,
      Libs.`play-json`.value,
      Libs.`scala-async`.value,
      Libs.`scala-java8-compat`,
      Akka.`akka-stream`,
      Akka.`akka-stream-typed`,
      Akka.`akka-actor`,
      Akka.`akka-actor-typed`,
      Libs.`akka-stream-kafka`,
      Libs.`lettuce`,
      Libs.`reactor-core`,
      Libs.`scalapb-runtime`,
      Akka.`akka-actor-testkit-typed` % Test,
      Akka.`akka-stream-testkit`      % Test,
      Libs.`scalatest`.value          % Test,
      Libs.`junit`                    % Test,
      Libs.`junit-interface`          % Test,
      Libs.`mockito-scala`            % Test,
      Libs.`embedded-redis`           % Test,
      Libs.`scalatest-embedded-kafka` % Test,
      Akka.`akka-multi-node-testkit`  % Test,
      Libs.HdrHistogram               % Test,
      Libs.testng                     % Test
    )
  )

  val EventCli = Def.setting(
    Seq(
      Enumeratum.`enumeratum`.value,
      AkkaHttp.`akka-http`,
      Akka.`akka-stream`,
      Akka.`akka-stream-typed`,
      Akka.`akka-actor`,
      Libs.`play-json`.value,
      Libs.`scopt`,
      Libs.`scala-csv`,
      Libs.`scala-async`.value,
      Libs.`scalatest`.value % Test,
      Libs.`embedded-redis`  % Test
    )
  )

  val AlarmApi = Def.setting(
    Seq(
      Enumeratum.`enumeratum`.value,
      Libs.`config`,
      Libs.`play-json`.value,
      Akka.`akka-actor`,
      Akka.`akka-actor-typed`,
      Akka.`akka-stream`,
      Akka.`akka-stream-typed`,
      Libs.`scalatest`.value % Test
    )
  )

  val AlarmClient = Def.setting(
    Seq(
      Libs.`lettuce`,
      Libs.`reactor-core`,
      Libs.`config`,
      Libs.`play-json`.value,
      Libs.`scala-async`.value,
      Libs.`json-schema-validator`,
      Libs.`scala-java8-compat`,
      Akka.`akka-actor`,
      Akka.`akka-actor-typed`,
      Akka.`akka-stream`,
      Akka.`akka-stream-typed`,
      Libs.`junit`           % Test,
      Libs.`junit-interface` % Test,
      Libs.`scalatest`.value % Test,
      Libs.`mockito-scala`   % Test
    )
  )

  val AlarmCli = Def.setting(
    Seq(
      Enumeratum.`enumeratum`.value,
      Libs.`config`,
      Akka.`akka-actor`,
      Akka.`akka-actor-typed`,
      Akka.`akka-stream`,
      Akka.`akka-stream-typed`,
      Libs.`scala-async`.value,
      AkkaHttp.`akka-http`,
      Libs.`scopt`,
      Libs.`scalatest`.value % Test,
      Libs.`embedded-redis`  % Test
    )
  )

  val Testkit = Def.setting(
    Seq(
      Libs.`config`,
      Akka.`akka-actor`,
      Akka.`akka-stream`,
      Akka.`akka-stream-typed`,
      Akka.`akka-actor-typed`,
      AkkaHttp.`akka-http`,
      Libs.`scala-reflect`,
      Libs.`scala-java8-compat`,
      Keycloak.`keycloak-adapter-core`,
      //TODO: make this as provided deps
      Libs.`scalatest`.value,
      Libs.`embedded-redis`,
      Libs.`junit`,
      Libs.`mockito-scala`
    )
  )

  val TimeClockJvm = Def.setting(
    Seq(
      Libs.`jna`,
      Libs.`scalatest`.value % Test,
      Libs.`junit-interface` % Test
    )
  )

  val TimeCore = Def.setting(
    Seq(
      Libs.`play-json`.value,
      Libs.`play-json-derived-codecs`.value,
      Libs.`scalatest`.value % Test,
      Libs.`junit-interface` % Test
    )
  )

  val TimeScheduler = Def.setting(
    Seq(
      Akka.`akka-actor`,
      Libs.`junit-interface`          % Test,
      Libs.`scalatest`.value          % Test,
      Libs.HdrHistogram               % Test,
      Akka.`akka-actor-testkit-typed` % Test
    )
  )

  val DatabaseClient = Def.setting(
    Seq(
      Akka.`akka-actor-typed`,
      Libs.`config`,
      Libs.`postgresql`,
      Libs.`scala-java8-compat`,
      Libs.`scala-async`.value,
      Libs.`hikaricp`,
      Jooq.`jooq`,
      Jooq.`jooq-meta`,
      Jooq.`jooq-codegen`,
      Libs.`scalatest`.value % Test,
      Akka.`akka-actor`      % Test,
      Libs.`junit`           % Test,
      Libs.`junit-interface` % Test,
      Libs.`otj-pg-embedded` % Test
    )
  )

  val CswInstalledAdapter = Def.setting(
    Seq(
      Libs.`config`,
      Typelevel.`cats-core`,
      Keycloak.`keycloak-installed`,
      Libs.`os-lib`,
      //(legacy dependencies) required*
      Libs.`scalatest`.value % Test,
      Libs.`mockito-scala`   % Test
    )
  )

  val CswAasCore = Def.setting(
    Seq(
      Libs.`jwt-play-json`,
      Libs.`play-json`.value,
      Libs.`config`,
      Keycloak.`keycloak-core`,
      Keycloak.`keycloak-adapter-core`,
      Keycloak.`keycloak-authz`,
      AkkaHttp.`akka-http`,
      Libs.`scala-async`.value,
      Typelevel.`cats-core`,
      //(legacy dependencies) required*
      Libs.`jboss-logging`,
      Libs.httpclient,
      Libs.`scalatest`.value % Test,
      Libs.`mockito-scala`   % Test
    )
  )

  val AuthAkkaHttpAdapter = Def.setting(
    Seq(
      Libs.`config`,
      AkkaHttp.`akka-http`,
      Typelevel.`cats-core`,
      Libs.`scalatest`.value       % Test,
      AkkaHttp.`akka-http-testkit` % Test,
      Libs.`mockito-scala`         % Test,
      //Libs.`play-json`,
      Libs.`play-json-derived-codecs`.value % Test,
      Libs.`akka-http-play-json`            % Test,
      Libs.`embedded-keycloak`              % Test,
      Akka.`akka-multi-node-testkit`        % Test
    )
  )

  val Commons = Def.setting(
    Seq(
      Akka.`akka-actor`,
      AkkaHttp.`akka-http`,
      Libs.`play-json`.value,
      Libs.`scalatest`.value % Test,
      Libs.`embedded-redis`  % Test
    )
  )

  val NetworkUtils = Def.setting(
    Seq(
      Libs.`scalatest`.value % Test,
      Libs.`mockito-scala`   % Test
    )
  )

  val Romaine = Def.setting(
    Seq(
      Libs.`lettuce`,
      Enumeratum.`enumeratum`.value,
      Libs.`reactor-core`,
      Libs.`reactive-streams`,
      Libs.`scala-async`.value,
      Libs.`scala-java8-compat`,
      Akka.`akka-stream`,
      Akka.`akka-stream-typed`,
      Akka.`akka-actor`,
      Libs.`scalatest`.value % Test
    )
  )

  val Examples = Def.setting(
    Seq(
      Enumeratum.`enumeratum`.value,
      Libs.`config`,
      Libs.`lettuce`,
      Jooq.`jooq`,
      Libs.`scala-async`.value,
      Akka.`akka-actor`,
      Akka.`akka-stream`,
      Akka.`akka-stream-typed`,
      Akka.`akka-actor-typed`,
      AkkaHttp.`akka-http`,
      AkkaHttp.`akka-http-cors`,
      Akka.`akka-actor-testkit-typed`,
      Libs.`scalatest`.value % Test,
      Libs.`junit`           % Test,
      Libs.`junit-interface` % Test
    )
  )

  val Benchmark = Def.setting(
    Seq(
      Libs.`config`,
      Akka.`akka-actor`,
      Akka.`akka-stream`,
      Akka.`akka-stream-typed`,
      Akka.`akka-actor-typed`,
      AkkaHttp.`akka-http`,
      Libs.`play-json`.value,
      Libs.`gson`,
      Jackson.`jackson-core`,
      Jackson.`jackson-databind`,
      Akka.`akka-actor-testkit-typed`,
      Libs.`scalatest`.value % Test
    )
  )

  val Integration = Def.setting(
    Seq(
      Akka.`akka-actor`,
      Akka.`akka-stream`,
      Akka.`akka-stream-typed`,
      Akka.`akka-actor-typed`,
      AkkaHttp.`akka-http`,
      Akka.`akka-actor-testkit-typed`,
      Libs.`scalatest`.value,
    )
  )

}
