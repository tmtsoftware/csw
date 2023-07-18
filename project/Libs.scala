import org.portablescala.sbtplatformdeps.PlatformDepsPlugin.autoImport._
import sbt.Def.{setting => dep}
import sbt._

object Libs {
  val ScalaVersion = "2.13.8"

  val `scala-java8-compat` = "org.scala-lang.modules" %% "scala-java8-compat" % "1.0.2" // BSD 3-clause "New" or "Revised" License
  val `scala-async`        = "org.scala-lang.modules" %% "scala-async"        % "1.0.1"
  val `scopt`              = "com.github.scopt"       %% "scopt"              % "4.0.1" // MIT License
  val `mockito`            = "org.scalatestplus"      %% "mockito-3-4"        % "3.2.10.0"

  // Dual license: Either, Eclipse Public License v1.0 or GNU Lesser General Public License version 2.1
  val `logback-classic` = "ch.qos.logback" % "logback-classic" % "1.2.10"

  val `embedded-keycloak` = "com.github.tmtsoftware.embedded-keycloak" %% "embedded-keycloak" % "0.6.0-pekko" // Apache 2.0
  val `pekko-management` = "org.apache.pekko" %% "pekko-management" % "0.0.0+758-aff55e07+20230718-0937-SNAPSHOT"
  val `svnkit`        = "org.tmatesoft.svnkit" % "svnkit"        % "1.10.3"     // TMate Open Source License
  val `commons-codec` = "commons-codec"        % "commons-codec" % "1.15"       // Apache 2.0š
  val `scala-reflect` = "org.scala-lang"       % "scala-reflect" % ScalaVersion // BSD-3
  val `gson`          = "com.google.code.gson" % "gson"          % "2.8.9"      // Apache 2.0
  val `play-json`     = "com.typesafe.play"   %% "play-json"     % "2.9.2"      // Apache 2.0

  val `enumeratum`      = dep("com.beachape" %%% "enumeratum" % "1.7.0")  // MIT License
  val `scala-java-time` = dep("io.github.cquiroz" %%% "scala-java-time" % "2.3.0")
  val `scalatest`       = dep("org.scalatest" %%% "scalatest" % "3.2.11") // Apache License 2.0

  val `jwt-core`         = "com.pauldijou"      %% "jwt-core"         % "5.0.0"
  val `lettuce`          = "io.lettuce"          % "lettuce-core"     % "6.1.6.RELEASE"
  val `reactor-core`     = "io.projectreactor"   % "reactor-core"     % "3.4.14"
  val `reactive-streams` = "org.reactivestreams" % "reactive-streams" % "1.0.3"
  // 2.1.1 version is breaking csw-event-client tests (pekko version based on 3.0.1)
  val `pekko-connectors-kafka` = "org.apache.pekko" %% "pekko-connectors-kafka" % "0.0.0+1764-b25806f7-SNAPSHOT"
  val `embedded-kafka`   = "io.github.embeddedkafka" %% "embedded-kafka"  % "3.0.0"
  val `embedded-redis`   = "com.github.kstyrc"        % "embedded-redis"  % "0.6"
  val `scala-compiler`   = "org.scala-lang"           % "scala-compiler"  % ScalaVersion
  val `HdrHistogram`     = "org.hdrhistogram"         % "HdrHistogram"    % "2.1.12"
  val `testng`           = "org.testng"               % "testng"          % "7.5.0"
  val `junit4-interface` = "com.github.sbt"           % "junit-interface" % "0.13.2"
  val `testng-6-7`       = "org.scalatestplus"       %% "testng-6-7"      % "3.2.10.0"

  val `scala-csv`             = "com.github.tototoshi" %% "scala-csv"             % "1.3.10"
  val `json-schema-validator` = "com.github.fge"        % "json-schema-validator" % "2.2.14" // LGPL/ASL

  val `jna`               = "net.java.dev.jna"              % "jna"               % "5.10.0"
  val `postgresql`        = "org.postgresql"                % "postgresql"        % "42.3.1"
  val `hikaricp`          = "com.zaxxer"                    % "HikariCP"          % "5.0.1" // Apache License 2.0
  val `io.zonky.test`     = "io.zonky.test"                 % "embedded-postgres" % "1.3.1"
  val httpclient          = "org.apache.httpcomponents"     % "httpclient"        % "4.5.13"
  val `jboss-logging`     = "org.jboss.logging"             % "jboss-logging"     % "3.4.3.Final"
  val `config`            = "com.typesafe"                  % "config"            % "1.4.1"
  val `os-lib`            = "com.lihaoyi"                  %% "os-lib"            % "0.8.0"
  val `caffeine`          = "com.github.ben-manes.caffeine" % "caffeine"          % "3.0.5"
  val netty               = "io.netty"                      % "netty-all"         % "4.1.73.Final"
  val `case-app`          = "com.github.alexarchambault"   %% "case-app"          % "2.0.6"
  val `tmt-test-reporter` = "com.github.tmtsoftware"       %% "rtm"               % "7dded9abcc"
}

object Borer {
  val Version = "1.7.2-pekko"
  val Org     = "io.bullet"

  val `borer-core`        = dep(Org %%% "borer-core" % Version)
  val `borer-derivation`  = dep(Org %%% "borer-derivation" % Version)
  val `borer-compat-pekko` = Org %% "borer-compat-pekko" % Version
}

object Jackson {
  val Version = "2.13.1"

  val `jackson-core`         = "com.fasterxml.jackson.core"    % "jackson-core"         % Version
  val `jackson-databind`     = "com.fasterxml.jackson.core"    % "jackson-databind"     % Version
  val `jackson-module-scala` = "com.fasterxml.jackson.module" %% "jackson-module-scala" % Version
}

object Pekko {
  val Version = "1.0.0" // all pekko is Apache License 2.0

  val `pekko-stream`              = "org.apache.pekko" %% "pekko-stream"              % Version
  val `pekko-stream-typed`        = "org.apache.pekko" %% "pekko-stream-typed"        % Version
  val `pekko-remote`              = "org.apache.pekko" %% "pekko-remote"              % Version
  val `pekko-stream-testkit`      = "org.apache.pekko" %% "pekko-stream-testkit"      % Version
  val `pekko-actor`               = "org.apache.pekko" %% "pekko-actor"               % Version
  val `pekko-actor-typed`         = "org.apache.pekko" %% "pekko-actor-typed"         % Version
  val `pekko-actor-testkit-typed` = "org.apache.pekko" %% "pekko-actor-testkit-typed" % Version
  val `pekko-distributed-data`    = "org.apache.pekko" %% "pekko-distributed-data"    % Version
  val `pekko-multi-node-testkit`  = "org.apache.pekko" %% "pekko-multi-node-testkit"  % Version
  val `pekko-cluster-tools`       = "org.apache.pekko" %% "pekko-cluster-tools"       % Version
  val `pekko-cluster`             = "org.apache.pekko" %% "pekko-cluster"             % Version
  val `pekko-cluster-typed`       = "org.apache.pekko" %% "pekko-cluster-typed"       % Version
  val `pekko-slf4j`               = "org.apache.pekko" %% "pekko-slf4j"               % Version
  val `cluster-sharding` =
    "org.apache.pekko" %% "pekko-cluster-sharding" % Version // required to maintaining the transitive dependency of akka-management-cluster-http
}

object PekkoHttp {
  val Version = "0.0.0+4470-61034832-SNAPSHOT"

  val `pekko-http`            = "org.apache.pekko" %% "pekko-http"            % Version
  val `pekko-http-testkit`    = "org.apache.pekko" %% "pekko-http-testkit"    % Version
  val `pekko-http-spray-json` = "org.apache.pekko" %% "pekko-http-spray-json" % Version

  val `pekko-http-cors`       = "org.apache.pekko" %% "pekko-http-cors"       % Version
}

object Keycloak {
  val Version = "16.1.0"

  val `keycloak-adapter-core` = "org.keycloak" % "keycloak-adapter-core"      % Version
  val `keycloak-core`         = "org.keycloak" % "keycloak-core"              % Version
  val `keycloak-installed`    = "org.keycloak" % "keycloak-installed-adapter" % Version
  val `keycloak-test-helper`  = "org.keycloak" % "keycloak-test-helper"       % Version
}

object Jooq {
  val Version = "3.16.2"

  val `jooq`         = "org.jooq" % "jooq"         % Version
  val `jooq-meta`    = "org.jooq" % "jooq-meta"    % Version
  val `jooq-codegen` = "org.jooq" % "jooq-codegen" % Version
}

object MSocket {
//  val Version = "0.6.0"
  val Version = "0.1.0-SNAPSHOT"

  val `msocket-api`      = dep("com.github.tmtsoftware.msocket" %%% "msocket-api" % Version)
  val `msocket-security` = "com.github.tmtsoftware.msocket" %% "msocket-security" % Version
  val `msocket-http`     = "com.github.tmtsoftware.msocket" %% "msocket-http"     % Version
}
