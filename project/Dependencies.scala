import sbt._

object Dependencies {

  val Messages = Def.setting(
    Seq(
      Libs.`scala-java8-compat`,
      Enumeratum.`enumeratum`.value,
      Libs.`play-json`.value,
      Libs.`play-json-extensions`,
      Enumeratum.`enumeratum-play`,
      Chill.`chill-bijection`,
      Libs.`scalapb-runtime`,
      Libs.`scalapb-json4s`,
      Libs.`upickle`.value,
      Akka.`akka-actor-typed`,
      Akka.`akka-cluster-tools`       % Test,
      Akka.`akka-actor-testkit-typed` % Test,
      Akka.`akka-actor`               % Test,
      Chill.`chill-akka`              % Test,
      Libs.`scalatest`.value          % Test,
      Libs.`junit`                    % Test,
      Libs.`junit-interface`          % Test
    )
  )

  val Params = Def.setting(
    Seq(
      Enumeratum.`enumeratum`.value,
      Enumeratum.`enumeratum-play-json`.value,
      Libs.`play-json`.value,
      Libs.`upickle`.value,
      Libs.`scalatest`.value % Test
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
      Akka.`akka-remote`,
      Akka.`akka-actor-typed`,
      Chill.`chill-akka`     % Test,
      Libs.`scalatest`.value % Test,
      Libs.`junit`           % Test,
      Libs.`junit-interface` % Test,
      Libs.`gson`            % Test
    )
  )

  val Benchmark = Def.setting(
    Seq(
      Libs.`persist-json`,
      Libs.`gson`,
      Jackson.`jackson-core`,
      Jackson.`jackson-databind`,
      Chill.`chill-akka`
    )
  )

  val Location = Def.setting(
    Seq(
      Akka.`akka-actor-typed`,
      Akka.`akka-actor-testkit-typed`,
      Akka.`akka-stream`,
      Akka.`akka-stream-typed`,
      Akka.`akka-distributed-data`,
      Akka.`akka-remote`,
      Akka.`akka-cluster-tools`,
      Akka.`akka-cluster-typed`,
      Libs.`scala-java8-compat`,
      Libs.`scala-async`,
      Enumeratum.`enumeratum`.value,
      Libs.`akka-management-cluster-http`,
      Chill.`chill-akka`,
      AkkaHttp.`akka-http`,
      Libs.`akka-http-upickle`,
      Libs.`upickle`.value,
      Libs.`scalatest`.value         % Test,
      Libs.`junit`                   % Test,
      Libs.`junit-interface`         % Test,
      Libs.`mockito-core`            % Test,
      Akka.`akka-stream-testkit`     % Test,
      Akka.`akka-multi-node-testkit` % Test
    )
  )

  val LocationAgent = Def.setting(
    Seq(
      Akka.`akka-actor`,
      Libs.`scopt`,
      Libs.`scalatest`.value % Test
    )
  )

  val ConfigClientCli = Def.setting(
    Seq(
      Akka.`akka-actor`,
      Libs.`scopt`,
      Libs.`scalatest`.value         % Test,
      Akka.`akka-multi-node-testkit` % Test
    )
  )

  val Integration = Def.setting(
    Seq(
      Libs.`scalatest`.value,
      Akka.`akka-stream-testkit`
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
      Libs.`scalatest`.value         % Test,
      Libs.`junit`                   % Test,
      Libs.`junit-interface`         % Test,
      Libs.`mockito-core`            % Test,
      Akka.`akka-multi-node-testkit` % Test,
      Akka.`akka-stream-testkit`     % Test
    )
  )

  val ClusterSeed = Def.setting(
    Seq(
      AkkaHttp.`akka-http`,
      Libs.`play-json`.value,
      Libs.`akka-http-play-json`,
      Libs.`scopt`,
      Libs.`scalatest`.value % Test
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

  val Command = Def.setting(
    Seq(
      Libs.`scala-async`,
      Akka.`akka-actor-typed`,
      Akka.`akka-actor-testkit-typed` % Test,
      Akka.`akka-stream-testkit`      % Test,
      Libs.`scalatest`.value          % Test,
      Chill.`chill-akka`              % Test,
      Libs.`junit`                    % Test,
      Libs.`junit-interface`          % Test,
      Libs.`mockito-core`             % Test
    )
  )

  val EventClient = Def.setting(
    Seq(
      Libs.`scala-async`,
      Akka.`akka-stream`,
      Libs.`akka-stream-kafka`,
      Libs.`lettuce`,
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
      Libs.`upickle`.value,
      Libs.`scopt`,
      Libs.`scala-csv`,
      Libs.`scalatest`.value % Test,
      Libs.`embedded-redis`  % Test
    )
  )
  val AlarmApi = Def.setting(
    Seq(
      Enumeratum.`enumeratum`.value,
      Libs.`upickle`.value,
      Akka.`akka-actor-typed`,
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
      Libs.`mockito-core`    % Test,
      Chill.`chill-akka`     % Test
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

  val Commons = Def.setting(
    Seq(
      Akka.`akka-stream`,
      AkkaHttp.`akka-http`,
      Libs.`play-json`.value,
      Libs.`scalatest`.value % Test,
      Libs.`embedded-redis`  % Test
    )
  )

  val Deploy = Def.setting(
    Seq(
      Libs.`scalatest`.value % Test
    )
  )

  val Examples = Def.setting(
    Seq(
      AkkaHttp.`akka-http`,
      Libs.`scalatest`.value % Test,
      Libs.`junit`           % Test,
      Libs.`junit-interface` % Test
    )
  )

  val SequencerPrototype = Def.setting(
    Seq(
      Libs.`scalatest`.value % Test,
      Akka.`akka-stream`,
      Akka.`akka-actor-typed`,
      Akka.`akka-actor-testkit-typed`,
      Libs.`scala-reflect`,
      Libs.`scala-compiler`,
      Ammonite.`ammonite`,
      Ammonite.`ammonite-sshd`
    )
  )

  val AcceptanceTests = Def.setting(
    Seq(
      Libs.`scalatest`.value
    )
  )
}
