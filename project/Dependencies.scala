import sbt._

object Dependencies {
  val scalatest = "org.scalatest" %% "scalatest" % "3.0.1"
  val `akka-stream` = "com.typesafe.akka" %% "akka-stream" % "2.4.17"
  val `jmdns` = "org.jmdns" % "jmdns" % "3.5.1"
}

object Akka {
  val Version = "2.4.17"
}
