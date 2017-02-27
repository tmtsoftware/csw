import Dependencies._

lazy val csw = project
  .in(file("."))
  .enablePlugins(UnidocSite, PublishGithub, PublishBintray)
  .aggregate(`csw-location`)
  .settings(Settings.mergeSiteWith(docs))

lazy val `csw-location` = project
  .enablePlugins(Coverage, PublishBintray)
  .settings(
    libraryDependencies += scalatest % Test
  )

lazy val docs = project
  .enablePlugins(ParadoxSite, NoPublish)
