import sbt._

object Dependencies {
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
    AkkaHttp.`akka-http`,
    Libs.`scalatest`               % Test,
    Libs.`junit`                   % Test,
    Libs.`junit-interface`         % Test,
    Libs.`mockito-core`            % Test,
    Akka.`akka-stream-testkit`     % Test,
    Akka.`akka-multi-node-testkit` % Test
  )

  val TrackLocationAgent = Seq(
    Akka.`akka-actor`,
    Libs.`scopt`,
    Libs.`scalatest`     % Test,
    Libs.`scala-logging` % Test
  )

  val ConfigCliClient = Seq(
    Akka.`akka-actor`,
    Libs.`scopt`,
    Libs.`scalatest`     % Test,
    Libs.`scala-logging` % Test
  )

  val Integration = Seq(
    Libs.`scalatest`,
    Akka.`akka-stream-testkit`
  )

  val Config = Seq(
    AkkaHttp.`akka-http`,
    AkkaHttp.`akka-http-spray-json`,
    Libs.svnkit,
    Libs.`scopt`,
    Libs.`commons-codec`,
    Libs.`scalatest`               % Test,
    AkkaHttp.`akka-http-testkit`   % Test,
    Libs.`junit`                   % Test,
    Libs.`junit-interface`         % Test,
    Akka.`akka-multi-node-testkit` % Test,
    Akka.`akka-stream-testkit`     % Test
  )
}
