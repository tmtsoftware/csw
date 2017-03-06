import sbt._

object Dependencies {
  val `scalatest` = "org.scalatest" %% "scalatest" % "3.0.1"
  val `scalamock-scalatest-support` = "org.scalamock" %% "scalamock-scalatest-support" % "3.5.0"
  val `akka-stream-testkit` = "com.typesafe.akka" %% "akka-stream-testkit" % "2.4.17"
  val `csw-location-local` = "org.tmt" %% "csw-location" % "latest.release"
}