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
      Libs.`scala-async`,
      Borer.`borer-compat-akka`.value,
      Libs.`scopt`,
      Libs.`scalatest`.value % Test
    )
  )

  val LocationModels = Def.setting(
    Seq(
      Libs.`enumeratum`.value,
      Libs.`play-json`.value,
      Borer.`borer-core`.value,
      Borer.`borer-derivation`.value,
      Libs.`scalatest`.value % Test,
      Libs.`mockito-scala`   % Test
    )
  )

  val LocationApi = Def.setting(
    Seq(
      Akka.`akka-actor`,
      Akka.`akka-actor-typed`,
      Akka.`akka-stream`,
      Akka.`akka-stream-typed`
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
      Libs.`scala-async`,
      Libs.`scopt`,
      Libs.`enumeratum`.value,
      Akka.`cluster-sharding`,
      Akka.`akka-persistence`,
      Libs.`akka-management-cluster-http`,
      AkkaHttp.`akka-http`,
      Borer.`borer-compat-akka`.value,
      Akka.`akka-actor-testkit-typed` % Test,
      Libs.`scalatest`.value          % Test,
      Libs.`junit`                    % Test,
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
      Libs.`scala-async`,
      Libs.`scala-java8-compat`,
      Borer.`borer-compat-akka`.value,
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

  val ConfigModels = Def.setting(
    Seq(
      Libs.`enumeratum`.value,
      Libs.`config`,
      Libs.`scala-java8-compat`,
      Libs.`scalatest`.value % Test
    )
  )

  val ConfigApi = Def.setting(
    Seq(
      Akka.`akka-actor`,
      Akka.`akka-stream`,
      Akka.`akka-stream-typed`,
      AkkaHttp.`akka-http`,
      Libs.`scalatest`.value     % Test,
      Akka.`akka-stream-testkit` % Test
    )
  )

  val ConfigServer = Def.setting(
    Seq(
      Libs.`enumeratum`.value,
      Akka.`akka-stream`,
      Akka.`akka-stream-typed`,
      Akka.`akka-actor`,
      Borer.`borer-compat-akka`.value,
      Libs.`scala-async`,
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
      Akka.`akka-actor`,
      Akka.`akka-stream`,
      Akka.`akka-stream-typed`,
      Borer.`borer-compat-akka`.value,
      AkkaHttp.`akka-http`,
      Libs.`scala-async`,
      Libs.`scala-java8-compat`,
      Libs.`scalatest`.value         % Test,
      Libs.`junit`                   % Test,
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

  val LoggingModels = Def.setting(
    Seq(
      Libs.`enumeratum`.value,
      Borer.`borer-core`.value,
      Borer.`borer-derivation`.value
    )
  )

  val LoggingClient = Def.setting(
    Seq(
      Libs.`config`,
      Libs.`logback-classic`,
      Libs.`play-json`.value,
      Libs.`scala-java8-compat`,
      Libs.`enumeratum`.value,
      Akka.`akka-actor`,
      Akka.`akka-actor-typed`,
      Libs.`scalatest`.value % Test,
      Libs.`junit`           % Test,
      Borer.`borer-core`.value,
      Libs.`gson` % Test
    )
  )

  val Params = Def.setting(
    Seq(
      Libs.`enumeratum`.value,
      Borer.`borer-core`.value,
      Borer.`borer-derivation`.value,
      Libs.`scalatest`.value % Test
    )
  )

  val ParamsJvm = Def.setting(
    Seq(
      Libs.`junit` % Test
    )
  )

  val Framework = Def.setting(
    Seq(
      Libs.`enumeratum`.value,
      Libs.`config`,
      Akka.`akka-actor`,
      Akka.`akka-stream`,
      Akka.`akka-stream-typed`,
      Libs.`scala-async`,
      Libs.`scala-java8-compat`,
      Libs.`play-json`.value,
      Akka.`akka-actor-typed`,
      Libs.`scopt`,
      Akka.`akka-actor-testkit-typed` % Test,
      Akka.`akka-stream-testkit`      % Test,
      Libs.`scalatest`.value          % Test,
      Libs.`junit`                    % Test,
      Libs.`mockito-scala`            % Test
    )
  )

  val CommandClient = Def.setting(
    Seq(
      Libs.`enumeratum`.value,
      Libs.`config`,
      Libs.`play-json`.value,
      Libs.`scala-java8-compat`,
      Akka.`akka-stream`,
      Akka.`akka-stream-typed`,
      Akka.`akka-actor`,
      Akka.`akka-actor-typed`,
      AkkaHttp.`akka-http`,
      Libs.`scala-async`,
      Libs.`caffeine`,
      Akka.`akka-actor-testkit-typed` % Test,
      Akka.`akka-stream-testkit`      % Test,
      Libs.`scalatest`.value          % Test,
      Libs.`junit`                    % Test,
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
      Libs.`enumeratum`.value,
      Libs.`config`,
      Libs.`scala-async`,
      Libs.`scala-java8-compat`,
      Akka.`akka-stream`,
      Akka.`akka-stream-typed`,
      Akka.`akka-actor`,
      Akka.`akka-actor-typed`,
      Libs.`akka-stream-kafka`,
      Libs.`lettuce`,
      Libs.`reactor-core`,
      Akka.`akka-actor-testkit-typed` % Test,
      Akka.`akka-stream-testkit`      % Test,
      Libs.`scalatest`.value          % Test,
      Libs.`junit`                    % Test,
      Libs.`mockito-scala`            % Test,
      Libs.`embedded-redis`           % Test,
//      Libs.`embedded-kafka`           % Test,
      Akka.`akka-multi-node-testkit` % Test,
      Libs.HdrHistogram              % Test,
      Libs.testng                    % Test
    )
  )

  val EventCli = Def.setting(
    Seq(
      Libs.`enumeratum`.value,
      AkkaHttp.`akka-http`,
      Akka.`akka-stream`,
      Akka.`akka-stream-typed`,
      Akka.`akka-actor`,
      Libs.`play-json`.value,
      Libs.`scopt`,
      Libs.`scala-csv`,
      Libs.`scala-async`,
      Libs.`scalatest`.value % Test,
      Libs.`embedded-redis`  % Test
    )
  )

  val AlarmModels = Def.setting(
    Seq(
      Libs.`enumeratum`.value,
      Libs.`config`,
      Libs.`play-json`.value,
      Libs.`scalatest`.value % Test
    )
  )

  val AlarmApi = Def.setting(
    Seq(
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
      Libs.`scala-async`,
      Libs.`json-schema-validator`,
      Libs.`scala-java8-compat`,
      Akka.`akka-actor`,
      Akka.`akka-actor-typed`,
      Akka.`akka-stream`,
      Akka.`akka-stream-typed`,
      Libs.`junit`           % Test,
      Libs.`scalatest`.value % Test,
      Libs.`mockito-scala`   % Test
    )
  )

  val AlarmCli = Def.setting(
    Seq(
      Libs.`enumeratum`.value,
      Libs.`config`,
      Akka.`akka-actor`,
      Akka.`akka-actor-typed`,
      Akka.`akka-stream`,
      Akka.`akka-stream-typed`,
      Libs.`scala-async`,
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
      Libs.`junit`           % Test
    )
  )

  val TimeCore = Def.setting(
    Seq(
      Libs.`play-json`.value,
      Libs.`scalatest`.value % Test,
      Libs.`junit`           % Test
    )
  )

  val TimeScheduler = Def.setting(
    Seq(
      Akka.`akka-actor`,
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
      Libs.`scala-async`,
      Libs.`hikaricp`,
      Jooq.`jooq`,
      Jooq.`jooq-meta`,
      Jooq.`jooq-codegen`,
      Libs.`scalatest`.value % Test,
      Akka.`akka-actor`      % Test,
      Libs.`junit`           % Test,
      Libs.`otj-pg-embedded` % Test
    )
  )

  val CswInstalledAdapter = Def.setting(
    Seq(
      Libs.`config`,
      Keycloak.`keycloak-installed`,
      Libs.`os-lib`,
      //(legacy dependencies) required*
      Libs.`scalatest`.value % Test,
      Libs.`mockito-scala`   % Test
    )
  )

  val CswAasCore = Def.setting(
    Seq(
      Libs.`jwt-core`,
      Libs.`play-json`.value,
      Libs.`config`,
      Keycloak.`keycloak-core`,
      Keycloak.`keycloak-adapter-core`,
      Keycloak.`keycloak-authz`,
      AkkaHttp.`akka-http`,
      Libs.`scala-async`,
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
      Libs.`play-json`.value,
      Libs.`scalatest`.value         % Test,
      AkkaHttp.`akka-http-testkit`   % Test,
      Libs.`mockito-scala`           % Test,
      Libs.`embedded-keycloak`       % Test,
      Akka.`akka-multi-node-testkit` % Test
    )
  )

  val Commons = Def.setting(
    Seq(
      Akka.`akka-actor`,
      AkkaHttp.`akka-http`,
      Borer.`borer-compat-akka`.value,
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
      Libs.`enumeratum`.value,
      Libs.`reactor-core`,
      Libs.`reactive-streams`,
      Libs.`scala-async`,
      Libs.`scala-java8-compat`,
      Akka.`akka-stream`,
      Akka.`akka-stream-typed`,
      Akka.`akka-actor`,
      Libs.`scalatest`.value % Test
    )
  )

  val Examples = Def.setting(
    Seq(
      Libs.`enumeratum`.value,
      Libs.`config`,
      Libs.`lettuce`,
      Jooq.`jooq`,
      Libs.`scala-async`,
      Akka.`akka-actor`,
      Akka.`akka-stream`,
      Akka.`akka-stream-typed`,
      Akka.`akka-actor-typed`,
      AkkaHttp.`akka-http`,
      AkkaHttp.`akka-http-cors`,
      Akka.`akka-actor-testkit-typed`,
      Libs.`scalatest`.value % Test,
      Libs.`junit`           % Test
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
      Libs.`scalatest`.value
    )
  )

}
