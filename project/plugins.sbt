addSbtPlugin("com.timushev.sbt"                  % "sbt-updates"               % "0.5.0")
addSbtPlugin("net.virtual-void"                  % "sbt-dependency-graph"      % "0.10.0-RC1")
addSbtPlugin("org.scalastyle"                    %% "scalastyle-sbt-plugin"    % "1.0.0")
addSbtPlugin("org.scalameta"                     % "sbt-scalafmt"              % "2.3.2")
addSbtPlugin("com.dwijnand"                      % "sbt-dynver"                % "4.0.0")
addSbtPlugin("com.thoughtworks.sbt-api-mappings" % "sbt-api-mappings"          % "3.0.0")
addSbtPlugin("org.foundweekends"                 % "sbt-bintray"               % "0.5.6")
addSbtPlugin("org.scoverage"                     % "sbt-scoverage"             % "1.6.1")
addSbtPlugin("com.typesafe.sbt"                  % "sbt-native-packager"       % "1.7.0")
addSbtPlugin("com.typesafe.sbt"                  % "sbt-multi-jvm"             % "0.4.0")
addSbtPlugin("com.eed3si9n"                      % "sbt-buildinfo"             % "0.9.0")
addSbtPlugin("pl.project13.scala"                % "sbt-jmh"                   % "0.3.7")
addSbtPlugin("com.orrsella"                      % "sbt-stats"                 % "1.0.7")
addSbtPlugin("de.johoop"                         % "sbt-testng-plugin"         % "3.1.1")
addSbtPlugin("io.spray"                          % "sbt-revolver"              % "0.9.1")
addSbtPlugin("com.typesafe.sbt"                  % "sbt-git"                   % "1.0.0")
addSbtPlugin("org.portable-scala"                % "sbt-scalajs-crossproject"  % "0.6.1")
addSbtPlugin("org.scala-js"                      % "sbt-scalajs"               % "0.6.32")
addSbtPlugin("com.github.cb372"                  % "sbt-explicit-dependencies" % "0.2.13")
addSbtPlugin("ohnosequences"                     % "sbt-github-release"        % "0.7.0")
addSbtPlugin("com.dwijnand"                      % "sbt-project-graph"         % "0.4.0")
addSbtPlugin("com.lightbend.paradox"             % "sbt-paradox"               % "0.6.9")
resolvers += Resolver.bintrayRepo("twtmt", "sbt-plugins")
addSbtPlugin("com.github.tmtsoftware" % "sbt-docs" % "0.1.4")

classpathTypes += "maven-plugin"

libraryDependencies += "com.sun.activation" % "javax.activation" % "1.2.0"

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
