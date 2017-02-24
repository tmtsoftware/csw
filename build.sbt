import Dependencies._

lazy val csw = project
  .in(file("."))
  .enablePlugins(Publish, PublishUnidoc, PublishGithub)
  .aggregate(`csw-location`, docs)
  .settings(
    (mappings in makeSite) := {
      (mappings in makeSite).value ++ (mappings in makeSite in docs).value
    }
  )

lazy val `csw-location` = project
  .enablePlugins(Publish, Coverage)
  .settings(
    libraryDependencies += scalatest % Test
  )

lazy val docs = project
  .enablePlugins(NoPublish, PublishParadox)
