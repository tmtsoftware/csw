import sbt._

object Dependencies {

  val Params = Seq(
    Libs.`spray-json`,
    Libs.`scala-java8-compat`,
    Libs.`scalatest`       % Test,
    Libs.`junit`           % Test,
    Libs.`junit-interface` % Test
  )

  val Logging = Seq(
    Libs.`logback-classic`,
    Libs.`persist-json`,
    Libs.`joda-time`,
    Libs.`enumeratum`,
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
    Libs.`jackson-core`,
    Libs.`jackson-databind`
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
    Libs.`enumeratum`,
    Libs.`chill-akka`,
    Libs.`akka-management-cluster-http`,
    Libs.`spray-json`,
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
    Libs.`enumeratum`,
    Akka.`akka-stream`,
    AkkaHttp.`akka-http-spray-json`,
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

  val Vslice = Seq(
    Libs.`scala-async`,
    Akka.`akka-typed`,
    Akka.`akka-typed-testkit` % Test,
    Libs.`scalatest`          % Test,
    Libs.`junit`              % Test,
    Libs.`junit-interface`    % Test,
    Libs.`mockito-core`       % Test
  )

  val CswClusterSeed = Seq(
    AkkaHttp.`akka-http`,
    Libs.`scopt`,
    Libs.`scalatest` % Test
  )

  val CswProdExamples = Seq(
    AkkaHttp.`akka-http`,
    Libs.`scalatest`       % Test,
    Libs.`junit`           % Test,
    Libs.`junit-interface` % Test
  )
}
