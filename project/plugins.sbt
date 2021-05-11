addSbtPlugin("com.timushev.sbt"                  % "sbt-updates"               % "0.5.3")
addSbtPlugin("org.scalastyle"                   %% "scalastyle-sbt-plugin"     % "1.0.0")
addSbtPlugin("org.scalameta"                     % "sbt-scalafmt"              % "2.4.2")
addSbtPlugin("com.dwijnand"                      % "sbt-dynver"                % "4.1.1")
addSbtPlugin("com.thoughtworks.sbt-api-mappings" % "sbt-api-mappings"          % "3.0.0")
addSbtPlugin("org.scoverage"                     % "sbt-scoverage"             % "1.7.3")
addSbtPlugin("com.typesafe.sbt"                  % "sbt-multi-jvm"             % "0.4.0")
addSbtPlugin("com.eed3si9n"                      % "sbt-buildinfo"             % "0.10.0")
addSbtPlugin("pl.project13.scala"                % "sbt-jmh"                   % "0.4.0")
addSbtPlugin("com.orrsella"                      % "sbt-stats"                 % "1.0.7")
addSbtPlugin("de.johoop"                         % "sbt-testng-plugin"         % "3.1.1")
addSbtPlugin("io.spray"                          % "sbt-revolver"              % "0.9.1")
addSbtPlugin("com.typesafe.sbt"                  % "sbt-git"                   % "1.0.0")
addSbtPlugin("org.portable-scala"                % "sbt-scalajs-crossproject"  % "1.0.0")
addSbtPlugin("org.scala-js"                      % "sbt-scalajs"               % "1.5.1")
addSbtPlugin("com.github.cb372"                  % "sbt-explicit-dependencies" % "0.2.16")
addSbtPlugin("com.dwijnand"                      % "sbt-project-graph"         % "0.4.0")
addSbtPlugin("com.timushev.sbt"                  % "sbt-rewarn"                % "0.1.3")

resolvers += "jitpack" at "https://jitpack.io"
libraryDependencies += "com.github.tmtsoftware" % "sbt-docs" % "9fe4596ff8"

classpathTypes += "maven-plugin"

scalacOptions ++= Seq(
  "-encoding",
  "UTF-8",
  "-feature",
  "-unchecked",
  "-deprecation",
  //"-Xfatal-warnings",
  "-Xlint:-unused,_",
  "-Ywarn-dead-code"
)
