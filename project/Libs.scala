import org.portablescala.sbtplatformdeps.PlatformDepsPlugin.autoImport._
import sbt.Def.{setting => dep}
import sbt._

object Libs {
  val ScalaVersion = "2.13.6"

  val `scala-java8-compat` = "org.scala-lang.modules" %% "scala-java8-compat" % "0.9.1" //BSD 3-clause "New" or "Revised" License
  val `scala-async`        = "org.scala-lang.modules" %% "scala-async"        % "1.0.0"
  val `scopt`              = "com.github.scopt"       %% "scopt"              % "4.0.1" //MIT License
  val `mockito-scala`      = "org.mockito"            %% "mockito-scala"      % "1.16.37"

  //Dual license: Either, Eclipse Public License v1.0 or GNU Lesser General Public License version 2.1
  val `logback-classic` = "ch.qos.logback" % "logback-classic" % "1.2.3"

  val `embedded-keycloak`            = "com.github.tmtsoftware.embedded-keycloak" %% "embedded-keycloak"            % "0.5.0"      //Apache 2.0
  val `akka-management-cluster-http` = "com.lightbend.akka.management"            %% "akka-management-cluster-http" % "1.1.1"
  val `svnkit`                       = "org.tmatesoft.svnkit"                      % "svnkit"                       % "1.10.3"     //TMate Open Source License
  val `commons-codec`                = "commons-codec"                             % "commons-codec"                % "1.15"       //Apache 2.0š
  val `scala-reflect`                = "org.scala-lang"                            % "scala-reflect"                % ScalaVersion //BSD-3
  val `gson`                         = "com.google.code.gson"                      % "gson"                         % "2.8.7"      //Apache 2.0
  val `play-json`                    = "com.typesafe.play"                        %% "play-json"                    % "2.9.2"      //Apache 2.0

  val `enumeratum`      = dep("com.beachape" %%% "enumeratum" % "1.7.0") //MIT License
  val `scala-java-time` = dep("io.github.cquiroz" %%% "scala-java-time" % "2.3.0")
  val `scalatest`       = dep("org.scalatest" %%% "scalatest" % "3.2.9") //Apache License 2.0

  val `jwt-core`          = "com.pauldijou"           %% "jwt-core"          % "5.0.0"
  val `lettuce`           = "io.lettuce"               % "lettuce-core"      % "6.1.3.RELEASE"
  val `reactor-core`      = "io.projectreactor"        % "reactor-core"      % "3.4.7"
  val `reactive-streams`  = "org.reactivestreams"      % "reactive-streams"  % "1.0.3"
  val `akka-stream-kafka` = "com.typesafe.akka"       %% "akka-stream-kafka" % "2.1.0"
  val `embedded-kafka`    = "io.github.embeddedkafka" %% "embedded-kafka"    % "2.8.0"
  val `embedded-redis`    = "com.github.kstyrc"        % "embedded-redis"    % "0.6"
  val `scala-compiler`    = "org.scala-lang"           % "scala-compiler"    % ScalaVersion
  val `HdrHistogram`      = "org.hdrhistogram"         % "HdrHistogram"      % "2.1.12"
  val `testng`            = "org.testng"               % "testng"            % "7.4.0"
  val `junit-4-13`        = "org.scalatestplus"       %% "junit-4-13"        % "3.2.9.0"
  val `testng-6-7`        = "org.scalatestplus"       %% "testng-6-7"        % "3.2.9.0"

  val `scala-csv`             = "com.github.tototoshi" %% "scala-csv"             % "1.3.8"
  val `json-schema-validator` = "com.github.fge"        % "json-schema-validator" % "2.2.6" //LGPL/ASL

  val `jna`               = "net.java.dev.jna"              % "jna"             % "5.8.0"
  val `postgresql`        = "org.postgresql"                % "postgresql"      % "42.2.22"
  val `hikaricp`          = "com.zaxxer"                    % "HikariCP"        % "4.0.3" //Apache License 2.0
  val `otj-pg-embedded`   = "com.opentable.components"      % "otj-pg-embedded" % "0.13.4"
  val httpclient          = "org.apache.httpcomponents"     % "httpclient"      % "4.5.13"
  val `jboss-logging`     = "org.jboss.logging"             % "jboss-logging"   % "3.4.2.Final"
  val `config`            = "com.typesafe"                  % "config"          % "1.4.1"
  val `os-lib`            = "com.lihaoyi"                  %% "os-lib"          % "0.7.8"
  val `caffeine`          = "com.github.ben-manes.caffeine" % "caffeine"        % "3.0.2"
  val netty               = "io.netty"                      % "netty-all"       % "4.1.65.Final"
  val `case-app`          = "com.github.alexarchambault"   %% "case-app"        % "2.0.6"
  val `tmt-test-reporter` = "com.github.tmtsoftware"       %% "rtm"             % "da9dc6cb"
}

object Borer {
  val Version = "1.7.2"
  val Org     = "io.bullet"

  val `borer-core`        = dep(Org %%% "borer-core" % Version)
  val `borer-derivation`  = dep(Org %%% "borer-derivation" % Version)
  val `borer-compat-akka` = Org %% "borer-compat-akka" % Version
}

object Jackson {
  val Version = "2.12.3"

  val `jackson-core`         = "com.fasterxml.jackson.core"    % "jackson-core"         % Version
  val `jackson-databind`     = "com.fasterxml.jackson.core"    % "jackson-databind"     % Version
  val `jackson-module-scala` = "com.fasterxml.jackson.module" %% "jackson-module-scala" % Version
}

object Akka {
  val Version = "2.6.15" //all akka is Apache License 2.0

  val `akka-stream`              = "com.typesafe.akka" %% "akka-stream"              % Version
  val `akka-stream-typed`        = "com.typesafe.akka" %% "akka-stream-typed"        % Version
  val `akka-remote`              = "com.typesafe.akka" %% "akka-remote"              % Version
  val `akka-stream-testkit`      = "com.typesafe.akka" %% "akka-stream-testkit"      % Version
  val `akka-actor`               = "com.typesafe.akka" %% "akka-actor"               % Version
  val `akka-actor-typed`         = "com.typesafe.akka" %% "akka-actor-typed"         % Version
  val `akka-actor-testkit-typed` = "com.typesafe.akka" %% "akka-actor-testkit-typed" % Version
  val `akka-distributed-data`    = "com.typesafe.akka" %% "akka-distributed-data"    % Version
  val `akka-multi-node-testkit`  = "com.typesafe.akka" %% "akka-multi-node-testkit"  % Version
  val `akka-cluster-tools`       = "com.typesafe.akka" %% "akka-cluster-tools"       % Version
  val `akka-cluster`             = "com.typesafe.akka" %% "akka-cluster"             % Version
  val `akka-cluster-typed`       = "com.typesafe.akka" %% "akka-cluster-typed"       % Version
  val `akka-slf4j`               = "com.typesafe.akka" %% "akka-slf4j"               % Version
  val `cluster-sharding` =
    "com.typesafe.akka" %% "akka-cluster-sharding" % Version //required to maintaining the transitive dependency of akka-management-cluster-http
}

object AkkaHttp {
  val Version = "10.2.4"

  val `akka-http`            = "com.typesafe.akka" %% "akka-http"            % Version //ApacheV2
  val `akka-http-testkit`    = "com.typesafe.akka" %% "akka-http-testkit"    % Version //ApacheV2
  val `akka-http-spray-json` = "com.typesafe.akka" %% "akka-http-spray-json" % Version

  val `akka-http-cors` = "ch.megard" %% "akka-http-cors" % "1.1.1"
}

object Keycloak {
  val Version = "14.0.0"

  val `keycloak-adapter-core` = "org.keycloak" % "keycloak-adapter-core"      % Version
  val `keycloak-core`         = "org.keycloak" % "keycloak-core"              % Version
  val `keycloak-installed`    = "org.keycloak" % "keycloak-installed-adapter" % Version
  val `keycloak-test-helper`  = "org.keycloak" % "keycloak-test-helper"       % Version
}

object Jooq {
  val Version = "3.14.11"

  val `jooq`         = "org.jooq" % "jooq"         % Version
  val `jooq-meta`    = "org.jooq" % "jooq-meta"    % Version
  val `jooq-codegen` = "org.jooq" % "jooq-codegen" % Version
}

object MSocket {
  val Version = "b012388205"

  val `msocket-api`      = dep("com.github.tmtsoftware.msocket" %%% "msocket-api" % Version)
  val `msocket-security` = "com.github.tmtsoftware.msocket" %% "msocket-security" % Version
  val `msocket-http`     = "com.github.tmtsoftware.msocket" %% "msocket-http"     % Version
}
