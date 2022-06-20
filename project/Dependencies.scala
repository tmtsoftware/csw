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
      Akka.`cluster-sharding`, // akka-management-cluster-http uses lower version, to avoid conflict, this needs to be overridden
      Libs.`akka-management-cluster-http`,
      AkkaHttp.`akka-http`,
      AkkaHttp.`akka-http-spray-json`, // akka-cluster-management uses lower version, to avoid conflict, this needs to be overridden
      MSocket.`msocket-http`,
      Libs.`scalatest`.value          % Test,
      Libs.`junit4-interface`         % Test,
      Libs.`mockito`                  % Test,
      Akka.`akka-actor-testkit-typed` % Test,
      Libs.`jboss-logging`            % Test,
      Libs.`embedded-keycloak`        % Test,
      Libs.netty                      % Test,
      Akka.`akka-stream-testkit`      % Test,
      AkkaHttp.`akka-http-testkit`    % Test
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
      MSocket.`msocket-http`,
      Libs.`scalatest`.value  % Test,
      Libs.`junit4-interface` % Test
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
      Borer.`borer-compat-akka`,
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
      Borer.`borer-compat-akka`,
      AkkaHttp.`akka-http`,
      Libs.`scala-async`,
      Libs.`scala-java8-compat`,
      Libs.`scalatest`.value  % Test,
      Libs.`junit4-interface` % Test,
      Libs.`mockito`          % Test
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
      Libs.`scala-java8-compat`,
      Libs.`enumeratum`.value,
      Akka.`akka-actor`,
      Akka.`akka-actor-typed`,
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
      Akka.`akka-actor`,
      Akka.`akka-stream`,
      Akka.`akka-stream-typed`,
      Libs.`scala-async`,
      Libs.`scala-java8-compat`,
      Libs.`play-json`,
      Akka.`akka-actor-typed`,
      Libs.`scopt`,
      Akka.`akka-actor-testkit-typed` % Test,
      Akka.`akka-stream-testkit`      % Test,
      Libs.`scalatest`.value          % Test,
      Libs.`junit4-interface`         % Test,
      Libs.`mockito`                  % Test
    )
  )

  val CommandClient = Def.setting(
    Seq(
      MSocket.`msocket-http`,
      Libs.`enumeratum`.value,
      Libs.`config`,
      Libs.`play-json`,
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
      Libs.`junit4-interface`         % Test,
      Libs.`mockito`                  % Test
    )
  )

  val CommandApi = Def.setting(
    Seq(
      Libs.`scala-async`,
      MSocket.`msocket-api`.value
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
      Libs.`HdrHistogram`             % Test,
      Akka.`akka-multi-node-testkit`  % Test,
      Akka.`akka-actor-testkit-typed` % Test,
      Libs.`scalatest`.value          % Test,
      Libs.`junit4-interface`         % Test,
      Libs.`mockito`                  % Test,
      Libs.`embedded-redis`           % Test,
      Libs.`embedded-kafka`           % Test,
      Libs.`testng-6-7`               % Test
    )
  )

  val EventCli = Def.setting(
    Seq(
      Libs.`enumeratum`.value,
      AkkaHttp.`akka-http`,
      Akka.`akka-stream`,
      Akka.`akka-stream-typed`,
      Akka.`akka-actor`,
      Libs.`play-json`,
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
      Libs.`play-json`,
      Libs.`scala-async`,
      Libs.`json-schema-validator`,
      Libs.`scala-java8-compat`,
      Akka.`akka-actor`,
      Akka.`akka-actor-typed`,
      Akka.`akka-stream`,
      Akka.`akka-stream-typed`,
      Libs.`scalatest`.value  % Test,
      Libs.`junit4-interface` % Test,
      Libs.`mockito`          % Test
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
      Akka.`akka-actor`,
      Libs.`jnr-ffi`,
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
      Libs.`scalatest`.value  % Test,
      Libs.`junit4-interface` % Test,
      Akka.`akka-actor`       % Test,
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
      AkkaHttp.`akka-http`,
      Libs.`scala-async`,
      // (legacy dependencies) required*
      Libs.`jboss-logging`,
      Libs.httpclient,
      Libs.`scalatest`.value % Test,
      Libs.`mockito`         % Test
    )
  )

  val AuthAkkaHttpAdapter = Def.setting(
    Seq(
      Libs.`config`,
      AkkaHttp.`akka-http`,
      Libs.`play-json`,
      Libs.`scalatest`.value          % Test,
      Akka.`akka-actor-testkit-typed` % Test,
      AkkaHttp.`akka-http-testkit`    % Test,
      Libs.`mockito`                  % Test
    )
  )

  val Commons = Def.setting(
    Seq(
      Akka.`akka-actor`              % Provided,
      Akka.`akka-actor-typed`        % Provided,
      AkkaHttp.`akka-http`           % Provided,
      Borer.`borer-compat-akka`      % Provided,
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
      Libs.`scalatest`.value  % Test,
      Libs.`junit4-interface` % Test
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
      Libs.`play-json`,
      Libs.`gson`,
      Jackson.`jackson-core`,
      Jackson.`jackson-databind`,
      Akka.`akka-actor-testkit-typed`,
      Libs.`scalatest`.value  % Test,
      Libs.`junit4-interface` % Test
    )
  )

  val Integration = Def.setting(
    Seq(
      Libs.`scalatest`.value,
      Libs.`junit4-interface`,
      Libs.`mockito`,
      Akka.`akka-actor`,
      Akka.`akka-actor-typed`,
      Akka.`akka-actor-testkit-typed`,
      Akka.`akka-stream`,
      Akka.`akka-stream-typed`,
      Akka.`akka-stream-testkit`,
      AkkaHttp.`akka-http`,
      Akka.`akka-multi-node-testkit`,
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
