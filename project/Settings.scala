import sbt._
import Keys._

object Settings {
  lazy val commonSettings = Seq(
    organization := "org.tmt",
    scalaVersion := "2.12.1",
    version      := "0.1.0-SNAPSHOT"
  )
}
