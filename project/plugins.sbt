addSbtPlugin("com.timushev.sbt"                  % "sbt-updates"               % "0.6.4")
//addSbtPlugin("org.scalastyle"                   %% "scalastyle-sbt-plugin"     % "1.0.0") // not scala 3 ready
addSbtPlugin("org.scalameta"                     % "sbt-scalafmt"              % "2.5.0")
addSbtPlugin("com.thoughtworks.sbt-api-mappings" % "sbt-api-mappings"          % "3.0.2")
addSbtPlugin("org.scoverage"                     % "sbt-scoverage"             % "2.0.6")
addSbtPlugin("com.typesafe.sbt"                  % "sbt-multi-jvm"             % "0.4.0")
addSbtPlugin("com.eed3si9n"                      % "sbt-buildinfo"             % "0.11.0")
addSbtPlugin("pl.project13.scala"                % "sbt-jmh"                   % "0.4.5")
addSbtPlugin("com.orrsella"                      % "sbt-stats"                 % "1.0.7")
addSbtPlugin("de.johoop"                         % "sbt-testng-plugin"         % "3.1.1")
addSbtPlugin("io.spray"                          % "sbt-revolver"              % "0.10.0")
addSbtPlugin("com.typesafe.sbt"                  % "sbt-git"                   % "1.0.2")
addSbtPlugin("org.portable-scala"                % "sbt-scalajs-crossproject"  % "1.2.0")
addSbtPlugin("org.scala-js"                      % "sbt-scalajs"               % "1.13.2")
addSbtPlugin("com.github.cb372"                  % "sbt-explicit-dependencies" % "0.2.16")
addSbtPlugin("com.timushev.sbt"                  % "sbt-rewarn"                % "0.1.3")
addSbtPlugin("de.heikoseeberger"                 % "sbt-header"                % "5.8.0")

addDependencyTreePlugin

ThisBuild / libraryDependencySchemes += "org.scala-lang.modules" %% "scala-xml" % VersionScheme.Always

resolvers += "jitpack" at "https://jitpack.io"
libraryDependencies += "com.github.tmtsoftware" % "sbt-docs" % "d3614b6"

classpathTypes += "maven-plugin"

scalacOptions ++= Seq(
  "-encoding",
  "UTF-8",
  "-feature",
  "-unchecked",
  "-deprecation",
  "-Xlint:-unused,_",
  "-Ywarn-dead-code"
)
