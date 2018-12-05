addSbtPlugin("com.thesamet"                      % "sbt-protoc"                 % "0.99.19")
addSbtPlugin("org.scalastyle"                    %% "scalastyle-sbt-plugin"     % "1.0.0")
addSbtPlugin("com.geirsson"                      % "sbt-scalafmt"               % "1.5.1")
addSbtPlugin("com.dwijnand"                      % "sbt-dynver"                 % "3.1.0")
addSbtPlugin("com.eed3si9n"                      % "sbt-unidoc"                 % "0.4.2")
addSbtPlugin("com.thoughtworks.sbt-api-mappings" % "sbt-api-mappings"           % "2.0.1")
addSbtPlugin("org.foundweekends"                 % "sbt-bintray"                % "0.5.4")
addSbtPlugin("com.timushev.sbt"                  % "sbt-updates"                % "0.3.4")
addSbtPlugin("com.typesafe.sbt"                  % "sbt-ghpages"                % "0.6.2")
addSbtPlugin("com.typesafe.sbt"                  % "sbt-site"                   % "1.3.2")
addSbtPlugin("org.scoverage"                     % "sbt-scoverage"              % "1.5.1")
addSbtPlugin("com.typesafe.sbt"                  % "sbt-native-packager"        % "1.3.15")
addSbtPlugin("com.typesafe.sbt"                  % "sbt-multi-jvm"              % "0.4.0")
addSbtPlugin("com.eed3si9n"                      % "sbt-buildinfo"              % "0.9.0")
addSbtPlugin("pl.project13.scala"                % "sbt-jmh"                    % "0.3.4")
addSbtPlugin("com.orrsella"                      % "sbt-stats"                  % "1.0.7")
addSbtPlugin("io.github.jonas"                   % "sbt-paradox-material-theme" % "0.4.0")
addSbtPlugin("de.johoop"                         % "sbt-testng-plugin"          % "3.1.1")
addSbtPlugin("io.spray"                          % "sbt-revolver"               % "0.9.1")
addSbtPlugin("com.typesafe.sbt"                  % "sbt-git"                    % "1.0.0")
addSbtPlugin("org.portable-scala"                % "sbt-scalajs-crossproject"   % "0.6.0")
addSbtPlugin("org.scala-js"                      % "sbt-scalajs"                % "0.6.26")
addSbtPlugin("com.github.kxbmap"                 % "sbt-jooq"                   % "0.4.0")

resolvers += "Jenkins repo" at "http://repo.jenkins-ci.org/public/"
addSbtPlugin("ohnosequences" % "sbt-github-release" % "0.7.0")

addSbtPlugin("io.get-coursier" % "sbt-coursier" % "1.1.0-M8")
classpathTypes += "maven-plugin"

libraryDependencies += "com.thesamet.scalapb" %% "compilerplugin" % "0.8.2"

scalacOptions ++= Seq(
  "-encoding",
  "UTF-8",
  "-feature",
  "-unchecked",
  "-deprecation",
  //"-Xfatal-warnings",
  "-Xlint",
  "-Yno-adapted-args",
  "-Ywarn-dead-code",
  "-Xfuture"
)
