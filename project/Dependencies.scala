import sbt._

object Dependencies {
  val `scalatest` = "org.scalatest" %% "scalatest" % "3.0.1"
  val `akka-stream` = "com.typesafe.akka" %% "akka-stream" % "2.4.17"
  val `jmdns` = "org.jmdns" % "jmdns" % "3.5.1"
  val `scala-java8-compat` = "org.scala-lang.modules" %% "scala-java8-compat" % "0.8.0"
  val `scalamock-scalatest-support` = "org.scalamock" %% "scalamock-scalatest-support" % "3.5.0"
}

object Akka {
  val Version = "2.4.17"
}
