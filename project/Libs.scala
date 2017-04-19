import sbt._

object Libs {
  val `scalatest`                    = "org.scalatest"              %% "scalatest"                    % "3.0.1" //Apache License 2.0
  val `scala-java8-compat`           = "org.scala-lang.modules"     %% "scala-java8-compat"           % "0.8.0" //BSD 3-clause "New" or "Revised" License
  val `scala-async`                  = "org.scala-lang.modules"     %% "scala-async"                  % "0.9.6" //BSD 3-clause "New" or "Revised" License
  val `scopt`                        = "com.github.scopt"           %% "scopt"                        % "3.5.0" //MIT License
  val `scala-logging`                = "com.typesafe.scala-logging" %% "scala-logging"                % "3.5.0" //Apache License 2.0
  val `acyclic`                      = "com.lihaoyi"                %% "acyclic"                      % "0.1.7" % Provided //MIT License
  val `enumeratum`                   = "com.beachape"               %% "enumeratum"                   % "1.5.8" //MIT License
  val `junit`                        = "junit"                      % "junit"                         % "4.12" //Eclipse Public License 1.0
  val `junit-interface`              = "com.novocode"               % "junit-interface"               % "0.11" //BSD 2-clause "Simplified" License
  val `mockito-core`                 = "org.mockito"                % "mockito-core"                  % "2.7.17" //MIT License
  val `logback-classic`              = "ch.qos.logback"             % "logback-classic"               % "1.1.3" % Runtime //Dual license: Either, Eclipse Public License v1.0 or GNU Lesser General Public License version 2.1
  val `chill-akka`                   = "com.twitter"                %% "chill-akka"                   % "0.9.2" //Apache License 2.0
  val `akka-management-cluster-http` = "com.lightbend.akka"         %% "akka-management-cluster-http" % "0.1-RC1" //N/A at the moment
  val svnkit                         = "org.tmatesoft.svnkit"       % "svnkit"                        % "1.8.11" //TMate Open Source License
  val `commons-codec`                = "commons-codec"              % "commons-codec"                 % "1.10" //Apache 2.0
}

object Akka {
  val Version                   = "2.5.0" //all akka is Apache License 2.0
  val `akka-stream`             = "com.typesafe.akka" %% "akka-stream" % Version
  val `akka-remote`             = "com.typesafe.akka" %% "akka-remote" % Version
  val `akka-stream-testkit`     = "com.typesafe.akka" %% "akka-stream-testkit" % Version
  val `akka-actor`              = "com.typesafe.akka" %% "akka-actor" % Version
  val `akka-distributed-data`   = "com.typesafe.akka" %% "akka-distributed-data" % Version
  val `akka-multi-node-testkit` = "com.typesafe.akka" %% "akka-multi-node-testkit" % Version
  val `akka-cluster-tools`      = "com.typesafe.akka" %% "akka-cluster-tools" % Version
}

object AkkaHttp {
  val Version                = "10.0.5"
  val `akka-http`            = "com.typesafe.akka" %% "akka-http" % Version //ApacheV2
  val `akka-http-spray-json` = "com.typesafe.akka" %% "akka-http-spray-json" % "10.0.5" //ApacheV2
  val `akka-http-testkit`    = "com.typesafe.akka" %% "akka-http-testkit" % "10.0.5" //ApacheV2
}
