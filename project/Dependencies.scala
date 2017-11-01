import sbt._

object Dependencies {

  val Messages = Seq(
    Libs.`scala-java8-compat`,
    Enumeratum.`enumeratum`,
    Libs.`play-json`,
    Libs.`play-json-extensions`,
    Enumeratum.`enumeratum-play`,
    Chill.`chill-bijection`,
    Libs.`scalapb-runtime`,
    Libs.`scalapb-json4s`,
    Akka.`akka-typed`,
    Akka.`akka-cluster-tools` % Test,
    Akka.`akka-typed-testkit` % Test,
    Akka.`akka-actor`         % Test,
    Chill.`chill-akka`        % Test,
    Libs.`scalatest`          % Test,
    Libs.`junit`              % Test,
    Libs.`junit-interface`    % Test
  )

  val Logging = Seq(
    Libs.`logback-classic`,
    Libs.`persist-json`,
    Libs.`joda-time`,
    Enumeratum.`enumeratum`,
    Akka.`akka-actor`,
    Akka.`akka-slf4j`,
    Akka.`akka-remote`,
    Akka.`akka-typed`,
    Libs.`scalatest`       % Test,
    Libs.`junit`           % Test,
    Libs.`junit-interface` % Test,
    Libs.`gson`            % Test
  )

  val Benchmark = Seq(
    Libs.`persist-json`,
    Libs.`gson`,
    Jackson.`jackson-core`,
    Jackson.`jackson-databind`,
    Chill.`chill-akka`
  )

  val Location = Seq(
    Akka.`akka-typed`,
    Akka.`akka-typed-testkit`,
    Akka.`akka-stream`,
    Akka.`akka-distributed-data`,
    Akka.`akka-remote`,
    Akka.`akka-cluster-tools`,
    Libs.`scala-java8-compat`,
    Libs.`scala-async`,
    Enumeratum.`enumeratum`,
    Libs.`akka-management-cluster-http`,
    Chill.`chill-akka`,
    AkkaHttp.`akka-http`,
    Libs.`scalatest`               % Test,
    Libs.`junit`                   % Test,
    Libs.`junit-interface`         % Test,
    Libs.`mockito-core`            % Test,
    Akka.`akka-stream-testkit`     % Test,
    Akka.`akka-multi-node-testkit` % Test
  )

  val CswLocationAgent = Seq(
    Akka.`akka-actor`,
    Libs.`scopt`,
    Libs.`scalatest` % Test
  )

  val CswConfigClientCli = Seq(
    Akka.`akka-actor`,
    Libs.`scopt`,
    Libs.`scalatest`               % Test,
    Akka.`akka-multi-node-testkit` % Test
  )

  val Integration = Seq(
    Libs.`scalatest`,
    Akka.`akka-stream-testkit`
  )

  val ConfigApi = Seq(
    Enumeratum.`enumeratum`,
    Akka.`akka-stream`,
    AkkaHttp.`akka-http-spray-json`,
    Libs.`spray-json`,
    Libs.`scalatest`           % Test,
    Akka.`akka-stream-testkit` % Test
  )

  val ConfigServer = Seq(
    AkkaHttp.`akka-http`,
    Libs.svnkit,
    Libs.`scopt`,
    Libs.`scalatest`             % Test,
    AkkaHttp.`akka-http-testkit` % Test,
    Akka.`akka-stream-testkit`   % Test
  )

  val ConfigClient = Seq(
    AkkaHttp.`akka-http`,
    Libs.`scalatest`               % Test,
    Libs.`junit`                   % Test,
    Libs.`junit-interface`         % Test,
    Libs.`mockito-core`            % Test,
    Akka.`akka-multi-node-testkit` % Test,
    Akka.`akka-stream-testkit`     % Test
  )

  val CswClusterSeed = Seq(
    AkkaHttp.`akka-http`,
    Libs.`spray-json`,
    Libs.`scopt`,
    Libs.`scalatest` % Test
  )

  val CswFramework = Seq(
    Libs.`scala-async`,
    Libs.`play-json`,
    Enumeratum.`enumeratum-play`,
    Akka.`akka-typed`,
    Libs.`scopt`,
    Akka.`akka-typed-testkit`  % Test,
    Akka.`akka-stream-testkit` % Test,
    Libs.`scalatest`           % Test,
    Libs.`junit`               % Test,
    Libs.`junit-interface`     % Test,
    Libs.`mockito-core`        % Test
  )

  val CswCommons = Seq(
    Akka.`akka-stream`,
    AkkaHttp.`akka-http`,
    Libs.`spray-json`
  )

  val CswProdExamples = Seq(
    AkkaHttp.`akka-http`,
    Libs.`scalatest`       % Test,
    Libs.`junit`           % Test,
    Libs.`junit-interface` % Test
  )
}
