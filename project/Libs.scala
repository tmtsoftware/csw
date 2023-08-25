import org.portablescala.sbtplatformdeps.PlatformDepsPlugin.autoImport._
import sbt.Def.{setting => dep}
import sbt._

object Libs {
//  val ScalaVersion = "3.3.1-RC5"
  val ScalaVersion = "3.3.0"

  val `dotty-cps-async` = dep("com.github.rssh" %%% "dotty-cps-async" % "0.9.18")

  val `scopt`   = "com.github.scopt"  %% "scopt"       % "4.1.0" // MIT License
  val `mockito` = "org.scalatestplus" %% "mockito-3-4" % "3.2.10.0"

  // Dual license: Either, Eclipse Public License v1.0 or GNU Lesser General Public License version 2.1
  val `logback-classic` = "ch.qos.logback" % "logback-classic" % "1.4.7"

  val `sourcecode`        = "com.lihaoyi"                              %% "sourcecode"        % "0.3.0"
  val `embedded-keycloak` = "com.github.tmtsoftware.embedded-keycloak" %% "embedded-keycloak" % "590c5e7" // Apache 2.0
  val `pekko-management`  = "org.apache.pekko"                         %% "pekko-management"  % "1.0.0"
  val `svnkit`        = "org.tmatesoft.svnkit" % "svnkit"        % "1.10.11"    // TMate Open Source License
  val `commons-codec` = "commons-codec"        % "commons-codec" % "1.15"       // Apache 2.0š
  val `gson`          = "com.google.code.gson" % "gson"          % "2.10.1"     // Apache 2.0
  val `play-json`     = "com.typesafe.play"   %% "play-json"     % "2.10.0-RC9" // Apache 2.0

  val `enumeratum`                = dep("com.beachape" %%% "enumeratum" % "1.7.3")  // MIT License
  val `scala-java-time`           = dep("io.github.cquiroz" %%% "scala-java-time" % "2.5.0")
  val `scalajs-java-securerandom` = dep("org.scala-js" %%% "scalajs-java-securerandom" % "1.0.0")
  val `scalatest`                 = dep("org.scalatest" %%% "scalatest" % "3.2.16") // Apache License 2.0

  val `jwt-core`               = "com.github.jwt-scala"    %% "jwt-core"               % "9.4.3"
  val `lettuce`                = "io.lettuce"               % "lettuce-core"           % "6.2.6.RELEASE"
  val `reactor-core`           = "io.projectreactor"        % "reactor-core"           % "3.5.9"
  val `reactive-streams`       = "org.reactivestreams"      % "reactive-streams"       % "1.0.4"
  val `pekko-connectors-kafka` = "org.apache.pekko"        %% "pekko-connectors-kafka" % "1.0.0"
  val `embedded-kafka`         = "io.github.embeddedkafka" %% "embedded-kafka"         % "3.5.1"
  val `embedded-redis`         = "com.github.kstyrc"        % "embedded-redis"         % "0.6"
  val `scala-compiler`         = "org.scala-lang"           % "scala-compiler"         % ScalaVersion
  val `HdrHistogram`           = "org.hdrhistogram"         % "HdrHistogram"           % "2.1.12"
  val `testng`                 = "org.testng"               % "testng"                 % "7.8.0"
  val `junit4-interface`       = "com.github.sbt"           % "junit-interface"        % "0.13.3"
  val `testng-6-7`             = "org.scalatestplus"       %% "testng-6-7"             % "3.2.10.0"

  val `scala-csv`             = "com.github.tototoshi" %% "scala-csv"             % "1.3.10"
  val `json-schema-validator` = "com.github.fge"        % "json-schema-validator" % "2.2.14" // LGPL/ASL

  val `jna`           = "net.java.dev.jna"          % "jna"               % "5.13.0"
  val `postgresql`    = "org.postgresql"            % "postgresql"        % "42.6.0"
  val `hikaricp`      = "com.zaxxer"                % "HikariCP"          % "5.0.1" // Apache License 2.0
  val `io.zonky.test` = "io.zonky.test"             % "embedded-postgres" % "2.0.4"
  val httpclient      = "org.apache.httpcomponents" % "httpclient"        % "4.5.14"
//  val httpclient          = "org.apache.httpcomponents.client5" % "httpclient5"   % "5.2.1"
  val `jboss-logging`     = "org.jboss.logging"             % "jboss-logging" % "3.5.3.Final"
  val `config`            = "com.typesafe"                  % "config"        % "1.4.2"
  val `os-lib`            = "com.lihaoyi"                  %% "os-lib"        % "0.9.1"
  val `caffeine`          = "com.github.ben-manes.caffeine" % "caffeine"      % "3.1.7"
  val netty               = "io.netty"                      % "netty-all"     % "4.1.95.Final"
  val `case-app`          = "com.github.alexarchambault"   %% "case-app"      % "2.1.0-M25"
  val `tmt-test-reporter` = "com.github.tmtsoftware.rtm"   %% "rtm"           % "f922171"
}

object Borer {
  val Version = "1.11.0"
//  val Version = "961eeed"
  val Org = "io.bullet"
//  val Org = "com.github.tmtsoftware.borer"

  val `borer-core`         = dep(Org %%% "borer-core" % Version)
  val `borer-derivation`   = dep(Org %%% "borer-derivation" % Version)
  val `borer-compat-pekko` = Org %% "borer-compat-pekko" % Version
}

object Jackson {
  val Version = "2.15.2"

  val `jackson-core`         = "com.fasterxml.jackson.core"    % "jackson-core"         % Version
  val `jackson-databind`     = "com.fasterxml.jackson.core"    % "jackson-databind"     % Version
  val `jackson-module-scala` = "com.fasterxml.jackson.module" %% "jackson-module-scala" % Version
}

object Pekko {
  val Version = "1.0.1" // all pekko is Apache License 2.0
  val Org     = "org.apache.pekko"

  val `pekko-stream`              = Org %% "pekko-stream"              % Version
  val `pekko-stream-typed`        = Org %% "pekko-stream-typed"        % Version
  val `pekko-remote`              = Org %% "pekko-remote"              % Version
  val `pekko-stream-testkit`      = Org %% "pekko-stream-testkit"      % Version
  val `pekko-actor`               = Org %% "pekko-actor"               % Version
  val `pekko-actor-typed`         = Org %% "pekko-actor-typed"         % Version
  val `pekko-actor-testkit-typed` = Org %% "pekko-actor-testkit-typed" % Version
  val `pekko-distributed-data`    = Org %% "pekko-distributed-data"    % Version
  val `pekko-multi-node-testkit`  = Org %% "pekko-multi-node-testkit"  % Version
  val `pekko-cluster-tools`       = Org %% "pekko-cluster-tools"       % Version
  val `pekko-cluster`             = Org %% "pekko-cluster"             % Version
  val `pekko-cluster-typed`       = Org %% "pekko-cluster-typed"       % Version
  val `pekko-slf4j`               = Org %% "pekko-slf4j"               % Version
  val `cluster-sharding` =
    Org %% "pekko-cluster-sharding" % Version // required to maintaining the transitive dependency of akka-management-cluster-http
}

object PekkoHttp {
  val Version = "1.0.0"
  val Org     = "org.apache.pekko"

  val `pekko-http`            = Org %% "pekko-http"            % Version
  val `pekko-http-testkit`    = Org %% "pekko-http-testkit"    % Version
  val `pekko-http-spray-json` = Org %% "pekko-http-spray-json" % Version
  val `pekko-http-cors`       = Org %% "pekko-http-cors"       % Version
  val `pekko-http-core`       = Org %% "pekko-http-core"       % Version
  val `pekko-http-parsing`    = Org %% "pekko-http-parsing"    % Version
}

object Keycloak {
//  val Version = "22.0.1"
  val Version = "16.1.0"

  val `keycloak-adapter-core`    = "org.keycloak" % "keycloak-adapter-core"      % Version
  val `keycloak-core`            = "org.keycloak" % "keycloak-core"              % Version
  val `keycloak-installed`       = "org.keycloak" % "keycloak-installed-adapter" % Version
  val `keycloak-test-helper`     = "org.keycloak" % "keycloak-test-helper"       % Version
  val `keycloak-policy-enforcer` = "org.keycloak" % "keycloak-policy-enforcer"   % Version
}

object Jooq {
  val Version = "3.18.6"

  val `jooq`         = "org.jooq" % "jooq"         % Version
  val `jooq-meta`    = "org.jooq" % "jooq-meta"    % Version
  val `jooq-codegen` = "org.jooq" % "jooq-codegen" % Version
}

object MSocket {
//  val Version = "0.6.0"
  val Version = "49741dd"

  val `msocket-api`      = dep("com.github.tmtsoftware.msocket" %%% "msocket-api" % Version)
  val `msocket-security` = "com.github.tmtsoftware.msocket" %% "msocket-security" % Version
  val `msocket-http`     = "com.github.tmtsoftware.msocket" %% "msocket-http"     % Version
}
