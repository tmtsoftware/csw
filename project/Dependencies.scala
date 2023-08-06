import sbt._

object Dependencies {

  val LocationApi = Def.setting(
    Seq(
      Libs.`enumeratum`.value,
      MSocket.`msocket-api`.value,
      Libs.`scalatest`.value % Test
    )
  )

  val LocationServer = Def.setting(
    Seq(
      Libs.`config`,
      Pekko.`pekko-actor`,
      Pekko.`pekko-actor-typed`,
      Pekko.`pekko-stream`,
      Pekko.`pekko-stream-typed`,
      Pekko.`pekko-cluster`,
      Pekko.`pekko-distributed-data`,
      Pekko.`pekko-cluster-typed`,
      PekkoHttp.`pekko-http-cors`,
      Libs.`dotty-cps-async`.value,
      Libs.`scopt`,
      Libs.`enumeratum`.value,
      Pekko.`cluster-sharding`, // akka-management-cluster-http uses lower version, to avoid conflict, this needs to be overridden
      Libs.`pekko-management`,
      PekkoHttp.`pekko-http`,
//      PekkoHttp.`pekko-http-spray-json`,
      MSocket.`msocket-http`,
      Libs.`scalatest`.value            % Test,
      Libs.`junit4-interface`           % Test,
      Libs.`mockito`                    % Test,
      Pekko.`pekko-actor-testkit-typed` % Test,
      Libs.`jboss-logging`              % Test,
      Libs.`embedded-keycloak`          % Test,
      Libs.netty                        % Test,
      Pekko.`pekko-stream-testkit`      % Test,
      PekkoHttp.`pekko-http-testkit`    % Test
    )
  )

  val LocationClient = Def.setting(
    Seq(
      Libs.`config`,
      Pekko.`pekko-actor`,
      Pekko.`pekko-stream`,
      Pekko.`pekko-stream-typed`,
      PekkoHttp.`pekko-http`,
      Pekko.`pekko-remote`,
      Libs.`dotty-cps-async`.value,
      MSocket.`msocket-http`,
      Libs.`scalatest`.value  % Test,
      Libs.`junit4-interface` % Test
    )
  )

  val LocationAgent = Def.setting(
    Seq(
      Libs.`config`,
      PekkoHttp.`pekko-http`,
      Pekko.`pekko-actor`,
      Pekko.`pekko-stream`,
      Pekko.`pekko-stream-typed`,
      Libs.`scopt`,
      Libs.`scalatest`.value % Test
    )
  )

  val ConfigModels = Def.setting(
    Seq(
      Libs.`enumeratum`.value,
      Libs.`config`,
      Libs.`scalatest`.value % Test
    )
  )

  val ConfigApi = Def.setting(
    Seq(
      Pekko.`pekko-actor`,
      Pekko.`pekko-stream`,
      Pekko.`pekko-stream-typed`,
      PekkoHttp.`pekko-http`,
      Libs.`scalatest`.value       % Test,
      Pekko.`pekko-stream-testkit` % Test
    )
  )

  val ConfigServer = Def.setting(
    Seq(
      Libs.`enumeratum`.value,
      Pekko.`pekko-stream`,
      Pekko.`pekko-stream-typed`,
      Pekko.`pekko-actor`,
      Borer.`borer-compat-pekko`,
      Libs.`dotty-cps-async`.value,
      Libs.`config`,
      PekkoHttp.`pekko-http`,
      PekkoHttp.`pekko-http-cors`,
      Libs.svnkit,
      Libs.`scopt`,
      Libs.`scalatest`.value         % Test,
      PekkoHttp.`pekko-http-testkit` % Test,
      Pekko.`pekko-stream-testkit`   % Test
    )
  )

  val ConfigClient = Def.setting(
    Seq(
      Libs.`config`,
      Pekko.`pekko-actor`,
      Pekko.`pekko-stream`,
      Pekko.`pekko-stream-typed`,
      Borer.`borer-compat-pekko`,
      PekkoHttp.`pekko-http`,
      Libs.`dotty-cps-async`.value,
      Libs.`scalatest`.value  % Test,
      Libs.`junit4-interface` % Test,
      Libs.`mockito`          % Test
    )
  )

  val ConfigCli = Def.setting(
    Seq(
      Libs.`config`,
      PekkoHttp.`pekko-http`,
      Pekko.`pekko-actor`,
      Pekko.`pekko-stream`,
      Pekko.`pekko-stream-typed`,
      Libs.`scopt`,
      Libs.`scalatest`.value % Test
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
      Libs.`play-json`,
      Libs.`enumeratum`.value,
      Pekko.`pekko-actor`,
      Pekko.`pekko-actor-typed`,
      Libs.`scalatest`.value  % Test,
      Libs.`junit4-interface` % Test,
      Borer.`borer-core`.value,
      Libs.`gson` % Test
    )
  )

  val Prefix = Def.setting(
    Seq(
      Libs.`enumeratum`.value,
      Borer.`borer-core`.value,
      Borer.`borer-derivation`.value,
      Libs.`scalatest`.value % Test
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
      Libs.`play-json`,
      Libs.`junit4-interface` % Test
    )
  )

  val Framework = Def.setting(
    Seq(
      Libs.`enumeratum`.value,
      Libs.`config`,
      Pekko.`pekko-actor`,
      Pekko.`pekko-stream`,
      Pekko.`pekko-stream-typed`,
      Libs.`dotty-cps-async`.value,
      Libs.`play-json`,
      Pekko.`pekko-actor-typed`,

      // temp: Added to resolve snapshot version conflict before pekko-http release available
      PekkoHttp.`pekko-http`,
      PekkoHttp.`pekko-http-core`,
      Libs.`scopt`,
      Pekko.`pekko-actor-testkit-typed` % Test,
      Pekko.`pekko-stream-testkit`      % Test,
      Libs.`scalatest`.value            % Test,
      Libs.`junit4-interface`           % Test,
      Libs.`mockito`                    % Test
    )
  )

  val CommandClient = Def.setting(
    Seq(
      MSocket.`msocket-http`,
      Libs.`enumeratum`.value,
      Libs.`config`,
      Libs.`play-json`,
      Pekko.`pekko-stream`,
      Pekko.`pekko-stream-typed`,
      Pekko.`pekko-actor`,
      Pekko.`pekko-actor-typed`,
      PekkoHttp.`pekko-http`,
      Libs.`dotty-cps-async`.value,
      Libs.`caffeine`,
      Pekko.`pekko-actor-testkit-typed` % Test,
      Pekko.`pekko-stream-testkit`      % Test,
      Libs.`scalatest`.value            % Test,
      Libs.`junit4-interface`           % Test,
      Libs.`mockito`                    % Test
    )
  )

  val CommandApi = Def.setting(
    Seq(
      Libs.`dotty-cps-async`.value,
      MSocket.`msocket-api`.value
    )
  )

  val EventApi = Def.setting(
    Seq(
      Pekko.`pekko-actor`,
      Pekko.`pekko-stream`,
      Pekko.`pekko-stream-typed`,
      Pekko.`pekko-actor-typed`
    )
  )

  val EventClient = Def.setting(
    Seq(
      Libs.`enumeratum`.value,
      Libs.`config`,
      Libs.`dotty-cps-async`.value,
      Pekko.`pekko-stream`,
      Pekko.`pekko-stream-typed`,
      Pekko.`pekko-actor`,
      Pekko.`pekko-actor-typed`,
      Libs.`pekko-connectors-kafka`,
      Libs.`lettuce`,
      Libs.`reactor-core`,
      Libs.`HdrHistogram`               % Test,
      Pekko.`pekko-multi-node-testkit`  % Test,
      Pekko.`pekko-actor-testkit-typed` % Test,
      Libs.`scalatest`.value            % Test,
      Libs.`junit4-interface`           % Test,
      Libs.`mockito`                    % Test,
      Libs.`embedded-redis`             % Test,
      Libs.`embedded-kafka`             % Test,
      Libs.`testng-6-7`                 % Test
    )
  )

  val EventCli = Def.setting(
    Seq(
      Libs.`enumeratum`.value,
      PekkoHttp.`pekko-http`,
      Pekko.`pekko-stream`,
      Pekko.`pekko-stream-typed`,
      Pekko.`pekko-actor`,
      Libs.`play-json`,
      Libs.`scopt`,
      Libs.`scala-csv`,
      Libs.`dotty-cps-async`.value,
      Libs.`scalatest`.value % Test,
      Libs.`embedded-redis`  % Test
    )
  )

  val AlarmModels = Def.setting(
    Seq(
      Libs.`enumeratum`.value,
      Libs.`config`,
      Libs.`scalatest`.value % Test
    )
  )

  val AlarmApi = Def.setting(
    Seq(
      Pekko.`pekko-actor`,
      Pekko.`pekko-actor-typed`,
      Pekko.`pekko-stream`,
      Pekko.`pekko-stream-typed`,
      Libs.`scalatest`.value % Test
    )
  )

  val AlarmClient = Def.setting(
    Seq(
      Libs.`lettuce`,
      Libs.`reactor-core`,
      Libs.`config`,
      Libs.`play-json`,
      Libs.`dotty-cps-async`.value,
      Libs.`json-schema-validator`,
      Pekko.`pekko-actor`,
      Pekko.`pekko-actor-typed`,
      Pekko.`pekko-stream`,
      Pekko.`pekko-stream-typed`,
      Libs.`scalatest`.value  % Test,
      Libs.`junit4-interface` % Test,
      Libs.`mockito`          % Test
    )
  )

  val AlarmCli = Def.setting(
    Seq(
      Libs.`enumeratum`.value,
      Libs.`config`,
      Pekko.`pekko-actor`,
      Pekko.`pekko-actor-typed`,
      Pekko.`pekko-stream`,
      Pekko.`pekko-stream-typed`,
      Libs.`dotty-cps-async`.value,
      PekkoHttp.`pekko-http`,
      Libs.`scopt`,
      Libs.`scalatest`.value % Test,
      Libs.`embedded-redis`  % Test
    )
  )

  val Testkit = Def.setting(
    Seq(
      Libs.`config`,
      Pekko.`pekko-actor`,
      Pekko.`pekko-stream`,
      Pekko.`pekko-stream-typed`,
      Pekko.`pekko-actor-typed`,
      PekkoHttp.`pekko-http`,
//      Libs.`scala-reflect`,
      Keycloak.`keycloak-adapter-core`,
      // TODO: make this as provided deps
      Libs.`scalatest`.value,
      Libs.`embedded-redis`,
      Libs.`io.zonky.test`,
      Libs.`junit4-interface`,
      Libs.`mockito`
    )
  )

  val TimeClockJvm = Def.setting(
    Seq(
      Libs.`jna`,
      Libs.`scalatest`.value  % Test,
      Libs.`junit4-interface` % Test
    )
  )

  val TimeScheduler = Def.setting(
    Seq(
      Pekko.`pekko-actor`,
      Libs.`scalatest`.value            % Test,
      Libs.HdrHistogram                 % Test,
      Pekko.`pekko-actor-testkit-typed` % Test
    )
  )

  val DatabaseClient = Def.setting(
    Seq(
      Pekko.`pekko-actor-typed`,
      Libs.`config`,
      Libs.`postgresql`,
      Libs.`dotty-cps-async`.value,
      Libs.`hikaricp`,
      Jooq.`jooq`,
      Jooq.`jooq-meta`,
      Jooq.`jooq-codegen`,
      Libs.`scalatest`.value  % Test,
      Libs.`junit4-interface` % Test,
      Pekko.`pekko-actor`     % Test,
      Libs.`io.zonky.test`    % Test
    )
  )

  val CswInstalledAdapter = Def.setting(
    Seq(
      Libs.`config`,
      Keycloak.`keycloak-installed`,
      Libs.`os-lib`,
      // (legacy dependencies) required*
      Libs.`scalatest`.value % Test,
      Libs.`mockito`         % Test
    )
  )

  val CswAasCore = Def.setting(
    Seq(
      Libs.`jwt-core`,
      Libs.`play-json`,
      Libs.`config`,
      MSocket.`msocket-security`,
      Keycloak.`keycloak-core`,
      Keycloak.`keycloak-adapter-core`,
      PekkoHttp.`pekko-http`,
      Libs.`dotty-cps-async`.value,
      // (legacy dependencies) required*
      Libs.`jboss-logging`,
      Libs.httpclient,
      Libs.`scalatest`.value % Test,
      Libs.`mockito`         % Test
    )
  )

  val AuthPekkoHttpAdapter = Def.setting(
    Seq(
      Libs.`config`,
      PekkoHttp.`pekko-http`,
      Libs.`play-json`,
      Libs.`scalatest`.value            % Test,
      Pekko.`pekko-actor-testkit-typed` % Test,
      PekkoHttp.`pekko-http-testkit`    % Test,
      Libs.`mockito`                    % Test
    )
  )

  val Commons = Def.setting(
    Seq(
      Pekko.`pekko-actor`            % Provided,
      Pekko.`pekko-stream`           % Provided,
      Pekko.`pekko-actor-typed`      % Provided,
      PekkoHttp.`pekko-http`         % Provided,
      Borer.`borer-compat-pekko`     % Provided,
      Borer.`borer-derivation`.value % Provided,
      Libs.`scalatest`.value         % Test,
      Libs.`embedded-redis`          % Test
    )
  )

  val NetworkUtils = Def.setting(
    Seq(
      Libs.`config`,
      Libs.`scalatest`.value % Test,
      Libs.`mockito`         % Test
    )
  )

  val Romaine = Def.setting(
    Seq(
      Libs.`lettuce`,
      Libs.`enumeratum`.value,
      Libs.`reactor-core`,
      Libs.`reactive-streams`,
      Libs.`dotty-cps-async`.value,
      Pekko.`pekko-stream`,
      Pekko.`pekko-stream-typed`,
      Pekko.`pekko-actor`,
      Libs.`scalatest`.value % Test
    )
  )

  val Examples = Def.setting(
    Seq(
      Libs.`enumeratum`.value,
      Libs.`config`,
      Libs.`lettuce`,
      Jooq.`jooq`,
      Libs.`dotty-cps-async`.value,
      Pekko.`pekko-actor`,
      Pekko.`pekko-stream`,
      Pekko.`pekko-stream-typed`,
      Pekko.`pekko-actor-typed`,
      PekkoHttp.`pekko-http`,
      PekkoHttp.`pekko-http-cors`,

      // temp: Added to resolve snapshot version conflict before pekko-http release available
      PekkoHttp.`pekko-http-core`,
      Pekko.`pekko-actor-testkit-typed`,
      Libs.`scalatest`.value  % Test,
      Libs.`junit4-interface` % Test
    )
  )

  val Benchmark = Def.setting(
    Seq(
      Libs.`config`,
      Pekko.`pekko-actor`,
      Pekko.`pekko-stream`,
      Pekko.`pekko-stream-typed`,
      Pekko.`pekko-actor-typed`,
      PekkoHttp.`pekko-http`,
      Libs.`play-json`,
      Libs.`gson`,
      Jackson.`jackson-core`,
      Jackson.`jackson-databind`,
      Pekko.`pekko-actor-testkit-typed`,
      Libs.`scalatest`.value  % Test,
      Libs.`junit4-interface` % Test
    )
  )

  val Integration = Def.setting(
    Seq(
      Libs.`scalatest`.value,
      Libs.`junit4-interface`,
      Libs.`mockito`,
      Pekko.`pekko-actor`,
      Pekko.`pekko-actor-typed`,
      Pekko.`pekko-actor-testkit-typed`,
      Pekko.`pekko-stream`,
      Pekko.`pekko-stream-typed`,
      Pekko.`pekko-stream-testkit`,
      PekkoHttp.`pekko-http`,
      Pekko.`pekko-multi-node-testkit`,
      Libs.`embedded-keycloak`,
      Libs.`lettuce`,
      Libs.`tmt-test-reporter`
    )
  )

  val Contract = Def.setting(
    Seq(
      Borer.`borer-core`.value,
      Libs.`play-json`,
      Libs.`scalatest`.value % Test
    )
  )

  val CswServices = Def.setting(
    Seq(
      Libs.`case-app`,
      Libs.`embedded-keycloak`
    )
  )

}
