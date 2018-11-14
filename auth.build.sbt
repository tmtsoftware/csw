/* ===================== Auth ================== */

lazy val `csw-auth` = project
  .in(file("csw-auth"))
  .aggregate(
    `native-client-adapter-scala`,
    `access-token`,
    `akka-http-adapter`,
    `keycloak-config`,
    `auth-examples`
  )

lazy val `native-client-adapter-scala` = project.settings(
  libraryDependencies ++= Dependencies.AuthNativeClientAdapter.value
).in(file("csw-auth/native-client-adapter-scala"))
  .dependsOn(`access-token`)

lazy val `access-token` = project.settings(
  libraryDependencies ++= Seq(
    Keycloak.`keycloak-adapter-core`,
    Libs.`jwt-play-json`,
    Libs.`config`
  )
).in(file("csw-auth/access-token"))
  .dependsOn(`keycloak-config`)

lazy val `akka-http-adapter` = project.settings(
  libraryDependencies ++= Seq(
    AkkaHttp.`akka-http`,
    Akka.`akka-stream`
  )
).in(file("csw-auth/akka-http-adapter"))
  .dependsOn(`access-token`)

lazy val `keycloak-config` = project.settings(
  libraryDependencies ++= Seq(
    Keycloak.`keycloak-adapter-core`,
    Keycloak.`keycloak-core`,
    Libs.`config`,

    //(legacy dependencies) required*
    Libs.`jboss-logging`,
    Libs.httpclient,
  )
).in(file("csw-auth/keycloak-config"))

lazy val `auth-examples` = project
  .in(file("csw-auth/auth-examples"))
  .aggregate(
    `akka-http-example`,
    `cli-app-example`
  )

lazy val `akka-http-example` = project.settings(
  libraryDependencies ++= Seq(
    AkkaHttp.`akka-http`,
    Akka.`akka-stream`
  )
).in(file("csw-auth/auth-examples/akka-http-example"))

lazy val `cli-app-example` = project
  .in(file("csw-auth/auth-examples/cli-app-example"))