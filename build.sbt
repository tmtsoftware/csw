import Dependencies._

lazy val csw = project
  .in(file("."))
  .enablePlugins(Publish, PublishUnidoc)
  .aggregate(`csw-location`)

lazy val `csw-location` = project
  .enablePlugins(Publish, Coverage)
  .settings(
    libraryDependencies += scalatest % Test
  )

lazy val docs = project
  .enablePlugins(PublishParadox, NoPublish)
