import sbt._

object Dependencies {

  val AdminServer = Def.setting(
    Seq(
      AkkaHttp.`akka-http`,
      Libs.`akka-http-play-json`,
      Libs.`scopt`,
      Libs.`scalatest`.value % Test
    )
  )

  val LocationApi = Def.setting(
    Seq(
      Akka.`akka-actor-typed`,
      Akka.`akka-stream`,
      Libs.`play-json`.value,
      Libs.`play-json-derived-codecs`.value,
      Libs.`scala-java8-compat`,
      Enumeratum.`enumeratum`.value,
      Libs.`scalatest`.value % Test,
      Libs.`mockito-core`    % Test
    )
  )

  val LocationServer = Def.setting(
    Seq(
      Akka.`akka-actor-typed`,
      Akka.`akka-actor-testkit-typed`,
      Akka.`akka-stream`,
      Akka.`akka-distributed-data`,
      Akka.`akka-remote`,
      Akka.`akka-cluster-tools`,
      Akka.`akka-cluster-typed`,
      AkkaHttp.`akka-http-cors`,
      Libs.`scala-java8-compat`,
      Libs.`scala-async`,
      Libs.`scopt`,
      Enumeratum.`enumeratum`.value,
      Libs.`akka-management-cluster-http`,
      AkkaHttp.`akka-http`,
      Libs.`akka-http-play-json`,
      Chill.`chill-akka`,
      Libs.`scalatest`.value         % Test,
      Libs.`junit`                   % Test,
      Libs.`junit-interface`         % Test,
      Libs.`mockito-core`            % Test,
      Akka.`akka-stream-testkit`     % Test,
      Akka.`akka-multi-node-testkit` % Test
    )
  )

  val LocationClient = Def.setting(
    Seq(
      AkkaHttp.`akka-http`,
      Akka.`akka-remote`,
      Libs.`scala-async`,
      Libs.`play-json`.value,
      Libs.`akka-http-play-json`,
      Libs.`scalatest`.value % Test
    )
  )

  val LocationAgent = Def.setting(
    Seq(
      Akka.`akka-actor`,
      Libs.`scopt`,
      Libs.`scalatest`.value % Test
    )
  )

  val ConfigApi = Def.setting(
    Seq(
      Enumeratum.`enumeratum`.value,
      Akka.`akka-stream`,
      Libs.`akka-http-play-json`,
      Libs.`play-json`.value,
      Libs.`scalatest`.value     % Test,
      Akka.`akka-stream-testkit` % Test
    )
  )

  val ConfigServer = Def.setting(
    Seq(
      AkkaHttp.`akka-http`,
      Libs.svnkit,
      Libs.`scopt`,
      Libs.`scalatest`.value       % Test,
      AkkaHttp.`akka-http-testkit` % Test,
      Akka.`akka-stream-testkit`   % Test
    )
  )

  val ConfigClient = Def.setting(
    Seq(
      AkkaHttp.`akka-http`,
      Libs.`scala-async`,
      Libs.`scalatest`.value         % Test,
      Libs.`junit`                   % Test,
      Libs.`junit-interface`         % Test,
      Libs.`mockito-core`            % Test,
      Akka.`akka-multi-node-testkit` % Test,
      Akka.`akka-stream-testkit`     % Test
    )
  )

  val ConfigCli = Def.setting(
    Seq(
      Akka.`akka-actor`,
      Libs.`scopt`,
      Libs.`scalatest`.value         % Test,
      Akka.`akka-multi-node-testkit` % Test
    )
  )

  val Logging = Def.setting(
    Seq(
      Libs.`logback-classic`,
      Libs.`persist-json`,
      Libs.`joda-time`,
      Enumeratum.`enumeratum`.value,
      Akka.`akka-actor`,
      Akka.`akka-slf4j`,
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
      Libs.`scala-async`,
      Libs.`play-json`.value,
      Enumeratum.`enumeratum-play`,
      Akka.`akka-actor-typed`,
      Libs.`scopt`,
      Akka.`akka-actor-testkit-typed` % Test,
      Akka.`akka-stream-testkit`      % Test,
      Libs.`scalatest`.value          % Test,
      Libs.`junit`                    % Test,
      Libs.`junit-interface`          % Test,
      Libs.`mockito-core`             % Test
    )
  )

  val CommandClient = Def.setting(
    Seq(
      Libs.`scala-async`,
      Akka.`akka-actor-typed`,
      Chill.`chill-akka`,
      Akka.`akka-actor-testkit-typed` % Test,
      Akka.`akka-stream-testkit`      % Test,
      Libs.`scalatest`.value          % Test,
      Libs.`junit`                    % Test,
      Libs.`junit-interface`          % Test,
      Libs.`mockito-core`             % Test
    )
  )

  val EventApi = Def.setting(
    Seq(
      Akka.`akka-stream`,
      Akka.`akka-actor-typed`
    )
  )

  val EventClient = Def.setting(
    Seq(
      Libs.`scala-async`,
      Akka.`akka-stream`,
      Libs.`akka-stream-kafka`,
      Libs.`lettuce`,
      Libs.`scalapb-runtime`,
      Akka.`akka-actor-testkit-typed` % Test,
      Akka.`akka-stream-testkit`      % Test,
      Libs.`scalatest`.value          % Test,
      Libs.`junit`                    % Test,
      Libs.`junit-interface`          % Test,
      Libs.`mockito-core`             % Test,
      Libs.`embedded-redis`           % Test,
      Libs.`scalatest-embedded-kafka` % Test,
      Akka.`akka-multi-node-testkit`  % Test,
      Libs.HdrHistogram               % Test,
      Libs.testng                     % Test
    )
  )

  val EventCli = Def.setting(
    Seq(
      Libs.`play-json`.value,
      Libs.`play-json-derived-codecs`.value,
      AkkaHttp.`akka-http`,
      Libs.`scopt`,
      Libs.`scala-csv`,
      Libs.`scalatest`.value % Test,
      Libs.`embedded-redis`  % Test
    )
  )

  val AlarmApi = Def.setting(
    Seq(
      Enumeratum.`enumeratum`.value,
      Libs.`play-json`.value,
      Libs.`play-json-derived-codecs`.value,
      Akka.`akka-actor-typed`,
      Akka.`akka-stream`,
      Libs.`scalatest`.value % Test
    )
  )

  val AlarmClient = Def.setting(
    Seq(
      Libs.`lettuce`,
      Libs.`scala-async`,
      Libs.`json-schema-validator`,
      Libs.`scala-java8-compat`,
      Akka.`akka-actor-typed`,
      Akka.`akka-stream`,
      Libs.`junit`           % Test,
      Libs.`junit-interface` % Test,
      Libs.`scalatest`.value % Test,
      Libs.`mockito-core`    % Test
    )
  )

  val AlarmCli = Def.setting(
    Seq(
      Libs.`scopt`,
      Libs.`scala-csv`,
      Libs.`scalatest`.value % Test,
      Libs.`embedded-redis`  % Test
    )
  )

  val Testkit = Def.setting(
    Seq(
      //TODO: make this as provided deps
      Libs.`scalatest`.value,
      Libs.`embedded-redis`,
      Libs.`junit`,
      Libs.`junit-interface`,
      Libs.`mockito-core`
    )
  )

  val ClockJvm = Def.setting(
    Seq(
      Libs.`jna`,
      Libs.`play-json-derived-codecs`.value,
      Libs.`scalatest`.value % Test,
      Libs.`junit-interface` % Test
    )
  )

  val TimeApi = Def.setting(
    Seq(
      Akka.`akka-actor`,
      Libs.`play-json-derived-codecs`.value,
      Libs.`scalatest`.value % Test,
      Libs.`junit-interface` % Test
    )
  )

  val TimeClient = Def.setting(
    Seq(
      Akka.`akka-actor`,
      Libs.`junit-interface`          % Test,
      Libs.`scalatest`.value          % Test,
      Akka.`akka-actor-testkit-typed` % Test
    )
  )

  val DatabaseClient = Def.setting(
    Seq(
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
      Libs.`junit-interface` % Test,
      Libs.`otj-pg-embedded` % Test
    )
  )

  val AuthNativeClientAdapter = Def.setting(
    Seq(
      Keycloak.`keycloak-installed`,
      Enumeratum.`enumeratum`.value,
      Libs.`os-lib`,
      //(legacy dependencies) required*
      Libs.`jboss-logging`,
      Libs.httpclient,
      Libs.`scalatest`.value % Test,
      Libs.`mockito-core`    % Test
    )
  )

  val CswAasCore = Def.setting(
    Seq(
      Libs.`jwt-play-json`,
      Libs.`config`,
      Keycloak.`keycloak-core`,
      Keycloak.`keycloak-adapter-core`,
      Keycloak.`keycloak-authz`,
      AkkaHttp.`akka-http`,
      Libs.`scala-async`,
      Typelevel.`cats-effects`,
      //(legacy dependencies) required*
      Libs.`jboss-logging`,
      Libs.httpclient,
      Libs.`scalatest`.value % Test,
      Libs.`mockito-core`    % Test
    )
  )

  val AuthAkkaHttpAdapter = Def.setting(
    Seq(
      AkkaHttp.`akka-http`,
      Akka.`akka-stream`,
      Libs.`scalatest`.value       % Test,
      AkkaHttp.`akka-http-testkit` % Test,
      Libs.`mockito-core`,
      //Libs.`play-json`,
      Libs.`play-json-derived-codecs`.value % Test,
      Libs.`akka-http-play-json`            % Test,
      Libs.`embedded-keycloak`              % Test,
      Akka.`akka-multi-node-testkit`        % Test
    )
  )

  val Commons = Def.setting(
    Seq(
      Akka.`akka-stream`,
      AkkaHttp.`akka-http`,
      Libs.`play-json`.value,
      Libs.`scalatest`.value % Test,
      Libs.`embedded-redis`  % Test
    )
  )

  val NetworkUtils = Def.setting(
    Seq(
      Libs.`scalatest`.value % Test,
      Libs.`mockito-core`    % Test
    )
  )

  val Romaine = Def.setting(
    Seq(
      Libs.`lettuce`,
      Enumeratum.`enumeratum`.value,
      Libs.`scala-async`,
      Libs.`scala-java8-compat`,
      Akka.`akka-stream`,
      Libs.`scalatest`.value % Test
    )
  )

  val Examples = Def.setting(
    Seq(
      AkkaHttp.`akka-http`,
      AkkaHttp.`akka-http-cors`,
      Libs.`scalatest`.value % Test,
      Libs.`junit`           % Test,
      Libs.`junit-interface` % Test
    )
  )

  val Benchmark = Def.setting(
    Seq(
      Libs.`persist-json`,
      Libs.`gson`,
      Jackson.`jackson-core`,
      Jackson.`jackson-databind`
    )
  )

  val Integration = Def.setting(
    Seq(
      Libs.`scalatest`.value,
      Akka.`akka-stream-testkit`
    )
  )

}
