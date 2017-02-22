import Dependencies._
import Settings._

lazy val csw = project
  .in(file("."))
  .enablePlugins(PublishUnidoc)
  .aggregate(`csw-location`)

lazy val `csw-location` = project
  .settings(commonSettings)
  .settings(
    libraryDependencies += scalatest % Test
  )

lazy val Local = config("local")

lazy val docs = project
  .enablePlugins(ParadoxPlugin, NoPublish)
  .disablePlugins(BintrayPlugin)
  .settings(
    name := "csw",
    inConfig(Compile)(defaultParadoxSettings),
    ParadoxPlugin.paradoxSettings(Local),
    inConfig(Local)(defaultParadoxSettings),
    paradoxProperties in Local ++= Map(
      // point API doc links to locally generated API docs
      "scaladoc.csw.base_url" -> rebase(
        (baseDirectory in csw).value, "../../../../../"
      )((sbtunidoc.Plugin.UnidocKeys.unidoc in csw in Compile).value.head).get
    )
  )
