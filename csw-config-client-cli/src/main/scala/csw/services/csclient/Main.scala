package csw.services.csclient

import akka.actor.ActorSystem
import csw.services.BuildInfo
import csw.services.csclient.cli.{ArgsParser, ClientCliWiring}
import csw.services.csclient.commons.ConfigClientCliLogger
import csw.services.location.commons.ClusterAwareSettings
import csw.services.logging.appenders.FileAppender
import csw.services.logging.scaladsl.LoggingSystem

import scala.concurrent.Await
import scala.concurrent.duration.DurationLong

/**
 * Application object allowing program execution from command line, also facilitates an entry point for Component level testing.
 */
class Main(actorSystem: ActorSystem) {
  def start(args: Array[String]): Unit =
    ArgsParser.parse(args).foreach { options ⇒
      val wiring = new ClientCliWiring(actorSystem)
      import wiring._
      try {
        options.op match {
          //adminApi
          case "create"             ⇒ commandLineRunner.create(options)
          case "update"             ⇒ commandLineRunner.update(options)
          case "get"                ⇒ commandLineRunner.get(options)
          case "delete"             ⇒ commandLineRunner.delete(options)
          case "list"               ⇒ commandLineRunner.list(options)
          case "history"            ⇒ commandLineRunner.history(options)
          case "historyActive"      ⇒ commandLineRunner.historyActive(options)
          case "setActiveVersion"   ⇒ commandLineRunner.setActiveVersion(options)
          case "resetActiveVersion" ⇒ commandLineRunner.resetActiveVersion(options)
          case "getActiveVersion"   ⇒ commandLineRunner.getActiveVersion(options)
          case "getActiveByTime"    ⇒ commandLineRunner.getActiveByTime(options)
          case "getMetadata"        ⇒ commandLineRunner.getMetadata(options)
          //clientApi
          case "exists"    ⇒ commandLineRunner.exists(options)
          case "getActive" ⇒ commandLineRunner.getActive(options)
          case x           ⇒ throw new RuntimeException(s"Unknown operation: $x")
        }
      } finally {
        Await.result(actorRuntime.shutdown(), 10.seconds)
      }
    }
}

object Main extends App with ConfigClientCliLogger.Simple {
  if (ClusterAwareSettings.seedNodes.isEmpty) {
    println(
      "clusterSeeds setting is not specified either as env variable or system property. Please check online documentation for this set-up."
    )
  } else {
    val actorSystem = ClusterAwareSettings.system
    new LoggingSystem(BuildInfo.name, BuildInfo.version, ClusterAwareSettings.hostname, Seq(FileAppender),
      system = actorSystem)

    new Main(actorSystem).start(args)
  }
}
