import sbt._
import Keys._
import org.scalastyle.sbt.ScalastylePlugin.scalastyle


object Settings {
//  lazy val compileScalastyle = taskKey[Unit]("compileScalastyle")
//  compileScalastyle := org.scalastyle.sbt.ScalastylePlugin.scalastyle.in(Compile).toTask("").value

  lazy val commonSettings = Seq(
    organization := "org.tmt",
    scalaVersion := "2.12.1",
    version      := "0.1.0-SNAPSHOT",
    (compile in Compile) := {
      scalastyle.inputTaskValue
      (compile in Compile).value
    }
  )
}