import sbt._

object Dependencies {

  val Logging = Seq(
    Libs.`logback-classic`,
    Libs.`persist-json`,
    Libs.`joda-time`,
    Akka.`akka-actor`,
    Akka.`akka-slf4j`,
    Libs.`scalatest` % Test
  )

  val Location = Seq(
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

  val CswClusterSeed = Seq(
    AkkaHttp.`akka-http`,
    Libs.`scopt`,
    Libs.`scalatest` % Test
  )
}
