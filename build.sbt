import Dependencies._

lazy val csw = project
  .in(file("."))
  .enablePlugins(PublishUnidoc)
  .aggregate(`csw-location`)

lazy val `csw-location` = project
  .settings(
    libraryDependencies += scalatest % Test
  )

lazy val docs = project
  .enablePlugins(ParadoxSitePlugin, GhpagesPlugin, NoPublish)
  .disablePlugins(BintrayPlugin)
  .settings(Settings.docsSettings(csw))
