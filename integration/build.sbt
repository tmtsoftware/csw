import Dependencies._

lazy val integration = project
  .in(file("."))
  .settings(
    libraryDependencies ++= Seq(
      `akka-stream-testkit` % Test,
      `scalatest` % Test,
      `scalamock-scalatest-support` % Test,
      `csw-location-local`,
      `track-location-local`,
      `jmdns`
    )
  )