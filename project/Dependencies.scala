import sbt._

object Dependencies {
  val `scalatest` = "org.scalatest" %% "scalatest" % "3.0.1"
  val `akka-stream` = "com.typesafe.akka" %% "akka-stream" % "2.4.17"
  val `jmdns` = "org.jmdns" % "jmdns" % "3.5.1"
  val `scala-java8-compat` = "org.scala-lang.modules" %% "scala-java8-compat" % "0.8.0"
  val `scalamock-scalatest-support` = "org.scalamock" %% "scalamock-scalatest-support" % "3.5.0"
  val `akka-remote` = "com.typesafe.akka" %% "akka-remote" % "2.4.17"
  val `akka-stream-testkit` = "com.typesafe.akka" %% "akka-stream-testkit" % "2.4.17"
  val `scala-async` = "org.scala-lang.modules" %% "scala-async" % "0.9.6"
  val `akka-actor` = "com.typesafe.akka" %% "akka-actor" % "2.4.17"
  val `scopt` = "com.github.scopt" %% "scopt" % "3.5.0"
  val `scala-logging` = "com.typesafe.scala-logging" %% "scala-logging" % "3.5.0"
  val `acyclic` = "com.lihaoyi" %% "acyclic" % "0.1.7" % "provided"
  val `enumeratum` = "com.beachape" %% "enumeratum" % "1.5.8"
  val `junit` = "junit" % "junit" % "4.12"
}

object Akka {
  val Version = "2.4.17"
}
